package com.xatkit.core.interpreter;

import com.xatkit.common.BooleanLiteral;
import com.xatkit.common.ConfigAccess;
import com.xatkit.common.ContextAccess;
import com.xatkit.common.Expression;
import com.xatkit.common.IfExpression;
import com.xatkit.common.ImportDeclaration;
import com.xatkit.common.Instruction;
import com.xatkit.common.Literal;
import com.xatkit.common.MatchedEventAccess;
import com.xatkit.common.MatchedIntentAccess;
import com.xatkit.common.NumberLiteral;
import com.xatkit.common.OperationCall;
import com.xatkit.common.Program;
import com.xatkit.common.SessionAccess;
import com.xatkit.common.StringLiteral;
import com.xatkit.common.VariableAccess;
import com.xatkit.common.VariableDeclaration;
import com.xatkit.core.ExecutionService;
import com.xatkit.core.interpreter.operation.Operation;
import com.xatkit.core.interpreter.operation.object.ObjectOperationProvider;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.EventInstance;
import com.xatkit.intent.RecognizedIntent;
import org.eclipse.emf.ecore.resource.Resource;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * Computes a Xatkit common {@link Program}.
 * <p>
 * This class provides utility methods to compute full {@link Program}s, as well as specific {@link Instruction}s
 * and {@link Expression}s. Note that computing a full {@link Program} ensures that the same {@link ExecutionContext}
 * is used along the computation.
 * <p>
 * <b>Note:</b> {@link CommonInterpreter} is stateless, and can be accessed through
 * {@link CommonInterpreter#getInstance()}. Note that the {@link ExecutionContext} used to compute and evaluate
 * expressions is not thread-safe.
 */
public class CommonInterpreter {

    /**
     * Returns the singleton instance of this class.
     * <p>
     * <b>Note:</b> {@link CommonInterpreter} is stateless, and can be accessed through
     * {@link CommonInterpreter#getInstance()}. Note that the {@link ExecutionContext} used to compute and evaluate
     * expressions is not thread-safe.
     *
     * @return the singleton instance of this class
     */
    public static CommonInterpreter getInstance() {
        if (isNull(INSTANCE)) {
            INSTANCE = new CommonInterpreter();
        }
        return INSTANCE;
    }

    /**
     * The singleton instance of this class.
     */
    private static CommonInterpreter INSTANCE = null;

    /**
     * The default {@link OperationProvider} used to compute {@link OperationCall} expressions.
     * <p>
     * This provider is used if there is no other available {@link OperationProvider} for a given source object.
     */
    private static OperationProvider DEFAULT_OPERATION_PROVIDER = new ObjectOperationProvider();

    /**
     * Computes the Xatkit common {@link Program} stored in the provided {@code resource}.
     * <p>
     * <b>Note:</b> this method creates an empty {@link ExecutionContext} to use along the computation. See
     * {@link #compute(Resource, ExecutionContext)} to compute a {@link Program} with a given {@link ExecutionContext}.
     * <p>
     * This method returns the evaluated value of the last instruction in the program.
     *
     * @param resource the {@link Resource} containing the {@link Program} to compute
     * @return the result of the computation
     * @throws NullPointerException     if the provided {@code resource} is {@code null}
     * @throws IllegalArgumentException if the provided {@code resource} does not contain a single instance of
     *                                  {@link Program}
     * @see #compute(Program)
     */
    public Object compute(Resource resource) {
        ExecutionContext context = new ExecutionContext();
        return compute(resource, context);
    }

    /**
     * Computes the Xatkit common {@link Program} stored in the provided {@code resource} with the given {@code
     * context}.
     * <p>
     * This method returns the evaluated value of the last instruction in the program.
     *
     * @param resource the {@link Resource} containing the {@link Program} to compute
     * @param context  the {@link ExecutionContext} to use along the computation
     * @return the result of the computation
     * @throws NullPointerException     if the provided {@code resource} or {@code context} is {@code null}
     * @throws IllegalArgumentException if the provided {@code resource} does not contain a single instance of
     *                                  {@link Program}
     * @see #compute(Resource, ExecutionContext)
     */
    public Object compute(Resource resource, ExecutionContext context) {
        checkNotNull(resource, "Cannot compute the program in the resource: %s", resource);
        checkArgument(resource.getContents().size() == 1 && resource.getContents().get(0) instanceof Program,
                "Cannot compute the program in the resource %s: the resource must contain a single instance of %s",
                resource, Program.class.getSimpleName());
        Program program = (Program) resource.getContents().get(0);
        return compute(program, context);
    }

    /**
     * Computes the provided Xatkit common {@link Program}.
     * <b>Note:</b> this method creates an empty {@link ExecutionContext} to use along the computation. See
     * {@link #compute(Program, ExecutionContext)} to compute a {@link Program} with a given {@link ExecutionContext}.
     * <p>
     * This method returns the evaluated value of the last instruction in the program.
     *
     * @param program the {@link Program} to compute
     * @return the result of the computation
     * @throws NullPointerException if the provided {@code program} is {@code null}
     * @see #compute(Program, ExecutionContext)
     */
    public Object compute(Program program) {
        ExecutionContext context = new ExecutionContext();
        return compute(program, context);
    }

    /**
     * Computes the provided Xatkit common {@code program} with the given {@code context}.
     * <p>
     * This method returns the evaluated value of the last instruction in the program.
     *
     * @param program the {@link Program} to compute
     * @param context the {@link ExecutionContext} to use along the computation
     * @return the result of the computation
     * @throws NullPointerException if the provided {@code program} or {@code context} is {@code null}
     * @see #compute(List, ExecutionContext)
     */
    public Object compute(Program program, ExecutionContext context) {
        checkNotNull(program, "Cannot compute the provided program %s", program);
        checkNotNull(context, "Cannot compute the program with the provided context %s", context);
        return compute(program.getInstructions(), context);
    }

    /**
     * Computes a list of {@link Instruction}s with the given {@code context}.
     * <p>
     * This method returns the evaluated value of the last instruction in the program.
     *
     * @param instructions the {@link List} of {@link Instruction}s to compute
     * @param context      the {@link ExecutionContext} to use along the computation
     * @return the result of the computation
     * @throws NullPointerException if the provided {@code instructions} is {@code null}
     */
    public Object compute(List<Instruction> instructions, ExecutionContext context) {
        checkNotNull(instructions, "Cannot compute the provided instruction list %s", instructions);
        Object result = null;
        for (Instruction i : instructions) {
            result = compute(i, context);
        }
        /*
         * Computing a list of instructions only returns the evaluated value of its last Instruction.
         * this may be improved by defining a custom result type that wraps additional information regarding the
         * execution.
         */
        return result;
    }

    /**
     * Computes the provided {@link Instruction} with the given {@code context}.
     *
     * @param i       the {@link Instruction} to compute
     * @param context the {@link ExecutionContext} to use along the computation
     * @return the result of the computation
     * @throws NullPointerException if the provided {@link Instruction} is {@code null}
     */
    public Object compute(Instruction i, ExecutionContext context) {
        checkNotNull(i, "Cannot compute the provided %s %s", Instruction.class.getSimpleName(), i);
        if (i instanceof ImportDeclaration) {
            // Do nothing, ImportDeclaration are not used by the interpreter
            return null;
        } else if (i instanceof VariableDeclaration) {
            return compute((VariableDeclaration) i, context);
        } else if (i instanceof Expression) {
            return evaluate((Expression) i, context);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute the instruction {0}, unknown " +
                    "expression type {1}", i, i.getClass().getSimpleName()));
        }
    }

    /**
     * Computes the provided {@link VariableDeclaration} {@link Instruction} with the given {@code context}.
     * <p>
     * This method evaluates the {@link VariableDeclaration}'s initialization expression and sets it in the provided
     * {@code context}.
     *
     * @param v       the {@link VariableDeclaration} to compute
     * @param context the {@link ExecutionContext} to use along the computation
     * @return the evaluated value of the {@link VariableDeclaration}
     * @see #evaluate(Expression, ExecutionContext)
     */
    public Object compute(VariableDeclaration v, ExecutionContext context) {
        Object value = v.getValue() == null ? null : evaluate(v.getValue(), context);
        context.setValue(v.getName(), value);
        return value;
    }

    /**
     * Evaluates the provided {@link Expression} with the given {@code context}.
     *
     * @param e       the {@link Expression} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the result of the evaluation
     * @throws NullPointerException if the provided {@link Expression} is {@code null}
     */
    public Object evaluate(Expression e, ExecutionContext context) {
        checkNotNull(e, "Cannot evaluate the provided %s %s", Expression.class.getSimpleName(), e);
        if (e instanceof VariableAccess) {
            return evaluate((VariableAccess) e, context);
        } else if (e instanceof ContextAccess) {
            return evaluate((ContextAccess) e, context);
        } else if (e instanceof SessionAccess) {
            return evaluate((SessionAccess) e, context);
        } else if (e instanceof ConfigAccess) {
            return evaluate((ConfigAccess) e, context);
        } else if (e instanceof MatchedEventAccess) {
            return evaluate((MatchedEventAccess) e, context);
        } else if (e instanceof MatchedIntentAccess) {
            return evaluate((MatchedIntentAccess) e, context);
        } else if (e instanceof Literal) {
            return evaluate((Literal) e, context);
        } else if (e instanceof OperationCall) {
            return evaluate((OperationCall) e, context);
        } else if (e instanceof IfExpression) {
            return evaluate((IfExpression) e, context);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot evaluate the expression {0}, unknown " +
                    "expression type {1}", e, e.getClass().getSimpleName()));
        }
    }

    /**
     * Evaluates the provided {@link VariableAccess} {@link Expression} and returns its value from the provided
     * {@code context}.
     *
     * @param v       the {@link VariableAccess} to evaluate
     * @param context the the {@link ExecutionContext} to retrieve the variable value from
     * @return the value of the variable associated to the provided {@link VariableAccess}
     */
    public Object evaluate(VariableAccess v, ExecutionContext context) {
        return context.getValue(v.getReferredVariable().getName());
    }

    /**
     * Evaluates the provided {@link ContextAccess} {@link Expression} and returns its value from the provided {@code
     * context}.
     *
     * @param c       the {@link ContextAccess} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the value of the context variable associated to the provided {@link ContextAccess}
     */
    public Object evaluate(ContextAccess c, ExecutionContext context) {
        return context.getSession().getRuntimeContexts().getContextVariables(c.getContextName());
    }

    /**
     * Evaluates the provided {@link SessionAccess} {@link Expression} and returns the corresponding
     * {@link XatkitSession}.
     *
     * @param s       the {@link SessionAccess} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the {@link XatkitSession} associated to the provided {@code context}
     */
    public Object evaluate(SessionAccess s, ExecutionContext context) {
        return context.getSession();
    }

    /**
     * Evaluates the provided {@link ConfigAccess} {@link Expression} and returns the corresponding
     * {@link org.apache.commons.configuration2.Configuration} value.
     *
     * @param c       the {@link ConfigAccess} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the {@link org.apache.commons.configuration2.Configuration} value if it exists, {@code null} otherwise
     */
    public Object evaluate(ConfigAccess c, ExecutionContext context) {
        if (isNull(context.getSession()) || isNull(context.getSession().getConfiguration())) {
            return null;
        }
        return context.getSession().getConfiguration().getProperty(c.getKeyName());
    }

    /**
     * Evaluates the provided {@link MatchedEventAccess} {@link Expression} and returns the corresponding
     * {@link EventInstance}.
     * <p>
     * This method looks in the {@link XatkitSession} for a value stored with the key
     * {@link ExecutionService#MATCHED_EVENT_SESSION_KEY} and returns it. If the {@link XatkitSession} is not defined
     * in the {@link ExecutionContext} this method returns {@code null}.
     *
     * @param e       the {@link MatchedEventAccess} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the retrieved {@link EventInstance}
     */
    public Object evaluate(MatchedEventAccess e, ExecutionContext context) {
        if (isNull(context.getSession())) {
            return null;
        }
        return context.getSession().get(ExecutionService.MATCHED_EVENT_SESSION_KEY);
    }

    /**
     * Evaluates the provided {@link MatchedIntentAccess} {@link Expression} and returns the corresponding
     * {@link RecognizedIntent}.
     * <p>
     * This method looks in the {@link XatkitSession} for a {@link RecognizedIntent} stored with the key
     * {@link ExecutionService#MATCHED_EVENT_SESSION_KEY} and returns it. If the {@link XatkitSession} is not defined
     * in the {@link ExecutionContext} or if it doesn't contain a {@link RecognizedIntent} (e.g. in case of an
     * {@link EventInstance} match) this method returns {@code null}.
     *
     * @param i       the {@link MatchedIntentAccess} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the retrieved {@link RecognizedIntent}
     */
    public Object evaluate(MatchedIntentAccess i, ExecutionContext context) {
        if (isNull(context.getSession())) {
            return null;
        }
        EventInstance eventInstance =
                (EventInstance) context.getSession().get(ExecutionService.MATCHED_EVENT_SESSION_KEY);
        if (eventInstance instanceof RecognizedIntent) {
            return eventInstance;
        } else {
            return null;
        }
    }

    /**
     * Evaluates the provided {@link Literal} {@link Expression}.
     *
     * @param l       the {@link Literal} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the value of the {@link Literal}
     */
    public Object evaluate(Literal l, ExecutionContext context) {
        if (l instanceof StringLiteral) {
            return evaluate((StringLiteral) l, context);
        } else if (l instanceof NumberLiteral) {
            return evaluate((NumberLiteral) l, context);
        } else if (l instanceof BooleanLiteral) {
            return evaluate((BooleanLiteral) l, context);
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Cannot compute the value of {0}, unknown literal" +
                    " type {1}", l, l.getClass().getSimpleName()));
        }
    }

    /**
     * Evaluates the provided {@link StringLiteral} {@link Expression}.
     *
     * @param l       the {@link StringLiteral} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the {@link String} value of the literal
     */
    public String evaluate(StringLiteral l, ExecutionContext context) {
        return l.getValue();
    }

    /**
     * Evaluates the provided {@link NumberLiteral} {@link Expression}.
     *
     * @param l       the {@link NumberLiteral} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the {@link Integer} value of the literal
     */
    public Integer evaluate(NumberLiteral l, ExecutionContext context) {
        return l.getValue();
    }

    /**
     * Evaluates the provided {@link BooleanLiteral} {@link Expression}.
     *
     * @param l       the {@link BooleanLiteral} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the {@link Boolean} value of the literal
     */
    public Boolean evaluate(BooleanLiteral l, ExecutionContext context) {
        return l.isValue();
    }

    /**
     * Evaluates the provided {@link OperationCall} {@link Expression}.
     * <p>
     * This method recursively evaluates the {@link OperationCall}'s source, and checks if there is an
     * {@link OperationProvider} available to return the concrete {@link Operation} from the provided
     * {@link OperationCall}.
     *
     * @param o       the {@link OperationCall} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the operation's result
     * @see OperationProvider
     * @see Operation
     */
    public Object evaluate(OperationCall o, ExecutionContext context) {
        Object source = evaluate(o.getSource(), context);
        Operation operation;
        if (source instanceof OperationProvider) {
            /*
             * The source object provides custom implementation of abstract operations, use it instead of the default
             * provider.
             */
            operation = ((OperationProvider) source).getOperation(o);
        } else {
            operation = DEFAULT_OPERATION_PROVIDER.getOperation(o);
        }
        List<Object> args = o.getArgs().stream()
                .map(arg -> evaluate(arg, context))
                .collect(Collectors.toList());
        return operation.invoke(source, args);
    }

    /**
     * Evaluates the provided {@link IfExpression}.
     * <p>
     * This method recursively evaluates the {@link IfExpression}'s condition, and computes the {@link Instruction}s
     * in the corresponding condition's branch.
     * <p>
     * <b>Note:</b> the evaluated value of an {@link IfExpression} is the last value returned by it's
     * {@link Instruction} list matching the if's condition.
     *
     * @param i       the {@link IfExpression} to evaluate
     * @param context the {@link ExecutionContext} to use along the evaluation
     * @return the if result
     * @throws IllegalArgumentException if the evaluated condition is not a {@link Boolean} value
     * @see #compute(List, ExecutionContext)
     */
    public Object evaluate(IfExpression i, ExecutionContext context) {
        Object condition = evaluate(i.getCondition(), context);
        checkArgument(condition instanceof Boolean, "Cannot evaluate if condition: %s (class=%s) is not a boolean " +
                "value", condition, condition.getClass().getSimpleName());
        Boolean boolCondition = (Boolean) condition;
        if (boolCondition.booleanValue()) {
            return compute(i.getThenInstructions(), context);
        } else {
            return compute(i.getElseInstructions(), context);
        }
    }
}
