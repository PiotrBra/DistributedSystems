import sys
import Ice
import Demo

# Klasa implementująca interfejs Example
class ExampleI(Demo.Example):
    def sayHello(self, current=None):
        return "Hello, World!"

    def add(self, a, b, current=None):
        return a + b

    def getPersons(self, current=None):
        return [Demo.Person("Alice", 30), Demo.Person("Bob", 25)]

# Inicjalizacja komunikatora Ice i uruchomienie serwera
with Ice.initialize(sys.argv) as communicator:
    adapter = communicator.createObjectAdapterWithEndpoints("ExampleAdapter", "default -p 10000")
    servant = ExampleI()
    adapter.add(servant, communicator.stringToIdentity("Example"))
    adapter.activate()
    print("Serwer uruchomiony na porcie 10000...")
    communicator.waitForShutdown()