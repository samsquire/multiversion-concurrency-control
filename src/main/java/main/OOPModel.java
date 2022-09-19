package main;

import java.util.HashMap;
import java.util.Map;

public class OOPModel {

    public static Type string = new Type("string");
    public static Type integer = new Type("integer");


    public static void main(String[] args) {
        Type rectangle = new Type("rectangle");
        rectangle.withField("width", integer);
        rectangle.withField("height", integer);
        rectangle.withMethod("area", new Method("area") {
            @Override
            public void run() {
                this.instance.returnValues.put("area", new Value(integer, (Integer) this.instance.values.get("width").value * (Integer) this.instance.values.get("height").value));
            }
        });
        Instance rectangle1 = rectangle.newInstance();
        rectangle1.withValue("width", integer, 5);
        rectangle1.withValue("height", integer, 5);
        System.out.println(rectangle1.call("area").value);
    }

    public static class Type {
        private final String name;
        private Map<String, Field> fields;
        private Map<String, Method> methods;
        public Type(String name) {
            this.name = name;
            this.fields = new HashMap<>();
            this.methods = new HashMap<>();
        }
        public Type withField(String name, Type type) {
            this.fields.put(name, new Field(name, type));
            return this;
        }
        public Type withMethod(String name, Method method) {
            this.methods.put(name, method);
            return this;
        }

        public Instance newInstance() {
            return new Instance(this);
        }
    }

    private static class Field {
        public String name;
        public Type type;

        public Field(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }

    private static class Method implements Runnable {

        public String name;
        public Instance instance;

        protected int value;

        public Method(String name) {
            this.name = name;
        }

        @Override
        public void run() {

        }
    }

    private static class Instance {
        private final Type type;
        public Map<String, Value> values;
        public Map<String, Type> types;
        public Map<String, Value> returnValues;
        public Instance(Type type) {
            this.type = type;
            this.values = new HashMap<>();
            this.types = new HashMap<>();
            this.returnValues = new HashMap<>();

        }
        public Instance withValue(String name, Type type, Object value) {
            this.values.put(name, new Value(type, value));
            this.types.put(name, type);
            return this;
        }

        public Value call(String methodName) {
            Method method = this.type.methods.get(methodName);
            method.instance = this;
            method.run();
            return returnValues.get(methodName);
        }
    }

    private static class Value {
        public Type type;
        public Object value;
        public Value(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}
