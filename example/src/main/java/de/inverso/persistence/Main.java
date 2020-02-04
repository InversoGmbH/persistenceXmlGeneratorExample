package de.inverso.persistence;

import de.inverso.persistencegenerator.example.Broker;

public class Main {

    public static void main(String[] args) {
        var em = DatabaseUtil.getEntityManager();
        var broker = new Broker();
        broker.setBrokerId("123");
        broker.setFirstName("foo");
        broker.setLastName("bar");
        em.getTransaction().begin();
        em.persist(broker);
        em.getTransaction().commit();
    }

}
