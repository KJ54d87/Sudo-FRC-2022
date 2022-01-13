package frc.auton.guiauto.serialization.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.wpilibj.DriverStation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendableCommand {

    @JsonProperty("methodName") public final String methodName;

    @JsonProperty("args") public final String[] args;

    @JsonProperty("argTypes") public final String[] argTypes;

    @JsonProperty("reflection") public final boolean reflection;

    private static final Map<String, Function<String, Object>> INFERABLE_TYPES_PARSER;

    static {
        INFERABLE_TYPES_PARSER = new HashMap<>();
        INFERABLE_TYPES_PARSER.put(int.class.getName(), Integer::parseInt);
        INFERABLE_TYPES_PARSER.put(double.class.getName(), Double::parseDouble);
        INFERABLE_TYPES_PARSER.put(float.class.getName(), Float::parseFloat);
        INFERABLE_TYPES_PARSER.put(long.class.getName(), Long::parseLong);
        INFERABLE_TYPES_PARSER.put(short.class.getName(), Short::parseShort);
        INFERABLE_TYPES_PARSER.put(byte.class.getName(), Byte::parseByte);
        INFERABLE_TYPES_PARSER.put(char.class.getName(), s -> s.charAt(0));
        INFERABLE_TYPES_PARSER.put(boolean.class.getName(), Boolean::parseBoolean);
        INFERABLE_TYPES_PARSER.put(String.class.getName(), s -> s);
        INFERABLE_TYPES_PARSER.put(Integer.class.getName(), Integer::valueOf);
        INFERABLE_TYPES_PARSER.put(Double.class.getName(), Double::valueOf);
        INFERABLE_TYPES_PARSER.put(Float.class.getName(), Float::valueOf);
        INFERABLE_TYPES_PARSER.put(Long.class.getName(), Long::valueOf);
        INFERABLE_TYPES_PARSER.put(Short.class.getName(), Short::valueOf);
        INFERABLE_TYPES_PARSER.put(Byte.class.getName(), Byte::valueOf);
        INFERABLE_TYPES_PARSER.put(Character.class.getName(), s -> Character.valueOf(s.charAt(0)));
        INFERABLE_TYPES_PARSER.put(Boolean.class.getName(), Boolean::valueOf);
    }


    @JsonCreator
    public SendableCommand(@JsonProperty("methodName") String methodName,
                           @JsonProperty("args") String[] args,
                           @JsonProperty("argTypes") String[] argTypes,
                           @JsonProperty("reflection") boolean reflection) {
        Method methodToCall = null;
        Object instance = null;

        this.methodName = methodName;
        this.args = args;
        this.argTypes = argTypes;
        this.reflection = reflection;

        objArgs = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            try {
                if (INFERABLE_TYPES_PARSER.containsKey(argTypes[i])) {
                    objArgs[i] = INFERABLE_TYPES_PARSER.get(argTypes[i]).apply(args[i]);
                } else {
                    objArgs[i] = Enum.valueOf(Class.forName(argTypes[i]).asSubclass(Enum.class), args[i]);
                }

            } catch (ClassNotFoundException e) {
                DriverStation.reportError("Class not found: " + argTypes[i], e.getStackTrace());
            } catch (ClassCastException e) {
                DriverStation.reportError("Could not cast " + argTypes[i] + " to an enum", e.getStackTrace());
            } finally {
                if (objArgs[i] == null) { // We failed to parse the argument
                    objArgs[i] = null;
                }
            }
        }

        if (reflection) {
            String[] splitMethod = methodName.split("\\.");

            String[] classNameArray = new String[splitMethod.length - 1];
            System.arraycopy(splitMethod, 0, classNameArray, 0, classNameArray.length);
            String className = String.join(".", classNameArray);

            try {
                Class<?> cls = Class.forName(className);
                methodToCall = cls.getMethod(splitMethod[splitMethod.length - 1],
                        Arrays.stream(objArgs).sequential().map(Object::getClass).toArray(Class[]::new));
                if (!Modifier.isStatic(methodToCall.getModifiers())) {
                    instance = cls.getMethod("getInstance").invoke(null);
                }
            } catch (ClassNotFoundException e) {
                DriverStation.reportError("Class not found: " + className, e.getStackTrace());
            } catch (NoSuchMethodException e) {
                DriverStation.reportError("Could not find method : " + splitMethod[splitMethod.length - 1] + " in class " + className, e.getStackTrace());
            } catch (InvocationTargetException | IllegalAccessException e) {
                DriverStation.reportError("Could not get singleton reference in class " + className + " for method: " +
                        splitMethod[splitMethod.length - 1], e.getStackTrace());
            }
        }

        this.methodToCall = methodToCall;
        this.instance = instance;
    }

    @JsonIgnoreProperties final Object instance;

    @JsonIgnoreProperties final Method methodToCall;

    @JsonIgnoreProperties final Object[] objArgs;


    /**
     * @return false if the command fails to execute
     */
    public boolean execute() {
        if (reflection) {
            try {
                methodToCall.invoke(instance, objArgs);
            } catch (IllegalAccessException e) {
                DriverStation.reportError("Could not access method " + methodName, e.getStackTrace());
                return false;
            } catch (InvocationTargetException e) {
                DriverStation.reportError("Method: " + methodName + " threw an exception while being invoked", e.getStackTrace());
                return false;
            }
        } else {
            try {
                switch (methodName) {
                    case "print":
                        System.out.println(objArgs[0]);
                        break;
                    case "sleep":
                        Thread.sleep((long) objArgs[0]);
                        break;
                }
            } catch (InterruptedException e) {
                DriverStation.reportError("Thread interrupted while sleeping", e.getStackTrace());
                return false;
            } catch (Exception e) {
                DriverStation.reportError("Could not invoke method " + methodName, e.getStackTrace());
                return false;
            }
        }
        return true;
    }
}
