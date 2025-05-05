module Demo {
    // Struktura reprezentująca osobę
    struct Person {
        string name;
        int age;
    };

    // Sekwencja (lista) struktur Person
    sequence<Person> PersonList;

    // Interfejs z trzema operacjami
    interface Example {
        string sayHello();            // Zwraca prosty tekst
        int add(int a, int b);        // Dodaje dwie liczby
        PersonList getPersons();      // Zwraca listę osób
    };
};