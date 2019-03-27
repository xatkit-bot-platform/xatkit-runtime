package edu.uoc.som.jarvis.core.interpreter;

import edu.uoc.som.jarvis.common.CommonFactory;
import edu.uoc.som.jarvis.common.CommonPackage;
import edu.uoc.som.jarvis.common.Program;
import edu.uoc.som.jarvis.core.interpreter.operation.OperationException;
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
