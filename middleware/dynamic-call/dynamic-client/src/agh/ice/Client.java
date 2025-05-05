package agh.ice;

import com.zeroc.Ice.*;

import java.lang.Object;
import java.util.*;

public class Client {
    public static void main(String[] args) {
        try (Communicator communicator = com.zeroc.Ice.Util.initialize(args)) {
            ObjectPrx proxy = communicator.stringToProxy("Example:tcp -h localhost -p 10000");

            // Invoke the operations available on the server
            invokeSayHello(proxy);
            invokeAdd(proxy, 5, 7);
            invokeGetPersons(proxy);
        } catch (LocalException e) {
            e.printStackTrace();
        }
    }

    // Invokes the sayHello operation
    private static void invokeSayHello(ObjectPrx proxy) {
        OutputStream out = new OutputStream(proxy.ice_getCommunicator());
        out.startEncapsulation();
        out.endEncapsulation();

        byte[] inParams = out.finished();
        com.zeroc.Ice.Object.Ice_invokeResult result = null;

        try {
            result = proxy.ice_invoke("sayHello", OperationMode.Normal, inParams, new HashMap<>());
        } catch (LocalException e) {
            e.printStackTrace();
            return;
        }

        if (result != null && result.outParams != null) {
            InputStream in = new InputStream(proxy.ice_getCommunicator(), result.outParams);
            in.startEncapsulation();
            String greeting = in.readString(); // Read the string return value
            in.endEncapsulation();
            System.out.println("Greeting: " + greeting);
        } else if (result != null && result.outParams == null) {
            System.err.println("Invocation successful for sayHello, but no output parameters received.");
        } else {
            System.err.println("Invocation failed for sayHello.");
        }
    }

    // Invokes the add operation
    private static void invokeAdd(ObjectPrx proxy, int a, int b) {
        OutputStream out = new OutputStream(proxy.ice_getCommunicator());
        out.startEncapsulation();
        out.writeInt(a);
        out.writeInt(b);
        out.endEncapsulation();

        byte[] inParams = out.finished();
        com.zeroc.Ice.Object.Ice_invokeResult result = null;

        try {
            result = proxy.ice_invoke("add", OperationMode.Normal, inParams, new HashMap<>());
        } catch (LocalException e) {
            e.printStackTrace();
            return;
        }

        if (result != null && result.outParams != null) {
            InputStream in = new InputStream(proxy.ice_getCommunicator(), result.outParams);
            in.startEncapsulation();
            int sum = in.readInt();
            in.endEncapsulation();
            System.out.println("Sum of " + a + " and " + b + ": " + sum);
        } else if (result != null && result.outParams == null) {
            System.err.println("Invocation successful for add, but no output parameters received.");
        } else {
            System.err.println("Invocation failed for add.");
        }
    }

    // Invokes the getPersons operation (no parameters, returns sequence of structs)
    private static void invokeGetPersons(ObjectPrx proxy) {
        OutputStream out = new OutputStream(proxy.ice_getCommunicator());
        out.startEncapsulation();
        // No input parameters for getPersons
        out.endEncapsulation();

        byte[] inParams = out.finished();
        com.zeroc.Ice.Object.Ice_invokeResult result = null;

        try {
            result = proxy.ice_invoke("getPersons", OperationMode.Normal, inParams, new HashMap<>());
        } catch (LocalException e) {
            e.printStackTrace();
            return;
        }

        if (result != null && result.outParams != null) {
            InputStream in = new InputStream(proxy.ice_getCommunicator(), result.outParams);
            in.startEncapsulation();
            int size = in.readSize(); // Read the size of the sequence
            List<Map<String, Object>> persons = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Map<String, Object> person = new HashMap<>();
                person.put("name", in.readString());
                person.put("age", in.readInt());
                persons.add(person);
            }
            in.endEncapsulation();

            System.out.println("Persons:");
            persons.forEach(person -> System.out.printf(
                    "Name: %s, Age: %d%n",
                    person.get("name"), person.get("age")
            ));
        } else if (result != null && result.outParams == null) {
            System.err.println("Invocation successful for getPersons, but no output parameters received.");
        }
        else {
            System.err.println("Invocation failed for getPersons.");
        }
    }
}