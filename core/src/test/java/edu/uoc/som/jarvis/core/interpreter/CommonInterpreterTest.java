package edu.uoc.som.jarvis.core.interpreter;

import edu.uoc.som.jarvis.common.CommonFactory;
import edu.uoc.som.jarvis.common.CommonPackage;
import edu.uoc.som.jarvis.common.Program;
import edu.uoc.som.jarvis.core.interpreter.operation.OperationException;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonInterpreterTest {

    private static String baseURI = "interpreter_inputs/";

    private static ResourceSet rSet;

    private CommonInterpreter interpreter;

    @BeforeClass
    public static void setUpBeforeClass() {
        EPackage.Registry.INSTANCE.put(CommonPackage.eNS_URI, CommonPackage.eINSTANCE);
        rSet = new ResourceSetImpl();
        rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
    }

    @Before
    public void setUp() {
        interpreter = new CommonInterpreter();
    }

    @Test(expected = NullPointerException.class)
    public void computeNullResource() {
        interpreter.compute((Resource) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeEmptyResource() {
        Resource resource = rSet.createResource(URI.createURI("test.xmi"));
        interpreter.compute(resource);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeResourceMultiplePrograms() {
        Resource resource = rSet.createResource(URI.createURI("test.xmi"));
        Program program1 = CommonFactory.eINSTANCE.createProgram();
        resource.getContents().add(program1);
        Program program2 = CommonFactory.eINSTANCE.createProgram();
        resource.getContents().add(program2);
        interpreter.compute(resource);
    }

    @Test
    public void string_literal() {
        Object result = interpreter.compute(getProgram("string_literal"));
        assertThat(result).as("correct String result").isEqualTo("Value");
    }

    @Test
    public void string_literal_length() {
        Object result = interpreter.compute(getProgram("string_literal_length"));
        assertThat(result).as("correct size result").isEqualTo("Value".length());
    }

    @Test
    public void int_literal() {
        Object result = interpreter.compute(getProgram("number_literal"));
        assertThat(result).as("correct Number result").isEqualTo(2);
    }

    @Test
    public void int_literal_toString() {
        Object result = interpreter.compute(getProgram("number_literal_toString"));
        assertThat(result).as("correct result type").isInstanceOf(String.class);
        assertThat(result).as("correct String result").isEqualTo("2");
    }

    @Test
    public void variable_declaration_no_value() {
        Object result = interpreter.compute(getProgram("variable_declaration_no_value"));
        assertThat(result).as("null result").isNull();
    }

    @Test
    public void variable_declaration_string_value() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("variable_declaration_string_value"), context);
        assertThat(result).as("correct String result").isEqualTo("Value");
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test
    public void variable_declaration_number_value() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("variable_declaration_number_value"), context);
        assertThat(result).as("correct Number value").isEqualTo(2);
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correrct Number " +
                "value").isEqualTo(2);
    }

    @Test
    public void variable_declaration_string_value_variable_access() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("variable_declaration_string_value_variable_access"), context);
        assertThat(result).as("correct String value").isEqualTo("Value");
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test(expected = OperationException.class)
    public void string_variable_size_call_invalid() {
        interpreter.compute(getProgram("string_variable_size_call_invalid"));
    }

    @Test
    public void string_variable_length_call() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("string_variable_length_call"), context);
        assertThat(result).as("correct length value").isEqualTo(5);
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        /*
         * Make sure the variable value hasn't been updated
         */
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test
    public void string_variable_toString_length() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("string_variable_toString_length"), context);
        assertThat(result).as("correct length value").isEqualTo(5);
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        /*
         * Make sure the variable value hasn't been updated
         */
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test(expected = OperationException.class)
    public void string_variable_equals_no_argument() {
        interpreter.compute(getProgram("string_variable_equals_no_argument"));
    }

    @Test(expected = OperationException.class)
    public void string_variable_equals_too_many_arguments() {
        interpreter.compute(getProgram("string_variable_equals_too_many_arguments"));
    }

    @Test
    public void string_variable_equals_true() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("string_variable_equals_true"), context);
        assertThat(result).as("result is true").isEqualTo(true);
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test
    public void string_variable_equals_false() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("string_variable_equals_false"), context);
        assertThat(result).as("result is false").isEqualTo(false);
        assertThat(context.getValueCount()).as("context contains a single variable").isEqualTo(1);
        assertThat(context.getValue("myVar")).as("context contains the declared variable with the correct String " +
                "value").isEqualTo("Value");
    }

    @Test
    public void string_variable_indexOf_two_arguments() {
        ExecutionContext context = new ExecutionContext();
        Object result = interpreter.compute(getProgram("string_variable_indexOf_two_arguments"), context);
        assertThat(result).as("result is 0").isEqualTo(0);
    }

    @Test
    public void context_access() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        session.getRuntimeContexts().setContextValue("test", 5, "var", "val");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("context_access"), context);
        assertThat(result).as("result is a RuntimeContexts instance").isInstanceOf(Map.class);
        Map<String, Object> contextValues = (Map<String, Object>) result;
        assertThat(contextValues.get("var")).as("result contains the correct value").isEqualTo("val");
    }

    @Test
    public void context_access_get() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        session.getRuntimeContexts().setContextValue("test", 5, "var", "val");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("context_access_get"), context);
        assertThat(result).as("correct context value result").isEqualTo("val");
    }

    @Test
    public void string_literal_plus_string_literal() {
        Object result = interpreter.compute(getProgram("string_literal_+_string_literal"));
        assertThat(result).as("valid concat result").isEqualTo("anothervalue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void string_literal_minus_string_literal() {
        interpreter.compute(getProgram("string_literal_-_string_literal"));
    }

    @Test
    public void number_literal_plus_number_literal() {
        Object result = interpreter.compute(getProgram("number_literal_+_number_literal"));
        assertThat(result).as("valid sum").isEqualTo(6);
    }

    @Test
    public void number_literal_minus_number_literal() {
        Object result = interpreter.compute(getProgram("number_literal_-_number_literal"));
        assertThat(result).as("valid substraction").isEqualTo(2);
    }

    @Test
    public void boolean_true_literal() {
        Object result = interpreter.compute(getProgram("boolean_true_literal"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void boolean_false_literal() {
        Object result = interpreter.compute(getProgram("boolean_false_literal"));
        assertThat(result).as("valid boolean (false) result").isEqualTo(false);
    }

    @Test
    public void false_and_true() {
        Object result = interpreter.compute(getProgram("false_and_true"));
        assertThat(result).as("valid boolean (false) result").isEqualTo(false);
    }

    @Test
    public void false_and_false() {
        Object result = interpreter.compute(getProgram("false_and_false"));
        assertThat(result).as("valid boolean (false) result").isEqualTo(false);
    }

    @Test
    public void true_and_false() {
        Object result = interpreter.compute(getProgram("true_and_false"));
        assertThat(result).as("valid boolean (false) result").isEqualTo(false);
    }

    @Test
    public void true_and_true() {
        Object result = interpreter.compute(getProgram("true_and_true"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void false_or_true() {
        Object result = interpreter.compute(getProgram("false_or_true"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void false_or_false() {
        Object result = interpreter.compute(getProgram("false_or_false"));
        assertThat(result).as("valid boolean (false) result").isEqualTo(false);
    }

    @Test
    public void true_or_false() {
        Object result = interpreter.compute(getProgram("true_or_false"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void true_or_true() {
        Object result = interpreter.compute(getProgram("true_or_true"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void if_true_then_true_else_false() {
        Object result = interpreter.compute(getProgram("if_true_then_true_else_false"));
        assertThat(result).as("valid boolean (true) result").isEqualTo(true);
    }

    @Test
    public void if_true_then_string_else_int() {
        Object result = interpreter.compute(getProgram("if_true_then_string_else_int"));
        assertThat(result).as("valid String result").isEqualTo("Value");
    }

    @Test
    public void if_false_then_string_else_int() {
        Object result = interpreter.compute(getProgram("if_false_then_string_else_int"));
        assertThat(result).as("valid int result").isEqualTo(2);
    }

    @Test
    public void nested_ifs() {
        Object result = interpreter.compute(getProgram("nested_ifs"));
        assertThat(result).as("valid String result").isEqualTo("Value2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void if_string_then_string_else_int() {
        interpreter.compute(getProgram("if_string_then_string_else_int"));
    }

    @Test
    public void session() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("session"), context);
        assertThat(result).as("valid JarvisSession result").isInstanceOf(JarvisSession.class);
        JarvisSession sessionResult = (JarvisSession) result;
        assertThat(sessionResult.getSessionId()).as("valid session ID").isEqualTo(session.getSessionId());
    }

    @Test
    public void session_get() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        session.store("test", "value");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("session_get"), context);
        assertThat(result).as("valid String result").isInstanceOf(String.class);
        assertThat(result).as("valid String value").isEqualTo("value");
    }

    @Test
    public void session_store() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("session_store"), context);
        assertThat(result).as("null result").isNull();
        assertThat(session.get("test")).as("valid session value").isEqualTo("value");
    }

    @Test
    public void session_store_get() {
        ExecutionContext context = new ExecutionContext();
        JarvisSession session = new JarvisSession("sessionID");
        context.setSession(session);
        Object result = interpreter.compute(getProgram("session_store_get"), context);
        assertThat(result).as("valid String result").isInstanceOf(String.class);
        assertThat(result).as("valid String value").isEqualTo("value");
        assertThat(session.get("test")).as("valid session value").isEqualTo("value");
    }

    @Test
    public void string_literal_isNull() {
        Object result = interpreter.compute(getProgram("string_literal_isNull"));
        assertThat(result).as("Result is false").isEqualTo(false);
    }

    @Test
    public void string_literal_nonNull() {
        Object result = interpreter.compute(getProgram("string_literal_nonNull"));
        assertThat(result).as("Result is true").isEqualTo(true);
    }

    private Resource getProgram(String fileName) {
        /*
         * Clear the previously loaded resources, just in case
         */
        rSet.getResources().clear();
        if (!fileName.endsWith(".xmi")) {
            /*
             * Avoid to repeat the ".xmi" in all the test cases
             */
            fileName = fileName + ".xmi";
        }
        URL resourceURL = CommonInterpreterTest.class.getClassLoader().getResource(baseURI + fileName);
        Resource resource = rSet.createResource(URI.createURI(baseURI + fileName));
        try {
            resource.load(resourceURL.openStream(), Collections.emptyMap());
        } catch (IOException e) {
            /*
             * Wrap the Exception to avoid catching it in all the tests.
             */
            throw new RuntimeException(e);
        }
        return resource;
    }
}
