package org.example;

import org.hibernate.Session;
import org.hibernate.dialect.lock.OptimisticEntityLockException;

import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Session session = HibernetUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<Item> itemList = session.createQuery("select i from Item i").getResultList();
        for (Item i: itemList) {
            i.setValue(0);
        }
        session.getTransaction().commit();
        CountDownLatch cdl = new CountDownLatch(8);
        for(int i = 0; i < 8; i++){
            new Thread(() -> {
                Session session_t = HibernetUtil.getSessionFactory().openSession();
                Random rnd = new Random();
                for(int j = 0;j < 20000; j++){
                    try {
                        session_t.beginTransaction();
                        Item item = session_t.get(Item.class, rnd.nextInt(40) + 1);
                        item.setValue(item.getValue() + 1);
                        session_t.getTransaction().commit();
                    }
                    catch (OptimisticLockException a){
                        session_t.getTransaction().rollback();
                        j--;
                    }
                    UncheckableSleep(5);
                }
                System.out.println("Thread ready");
                session_t.close();
                cdl.countDown();
            }).start();
        }
        cdl.await();
        List res = session.createQuery("select sum(i.value) from Item i").getResultList();
        System.out.println("Сумма всех обьектов " + res.get(0));
        session.close();
    }
    public static void UncheckableSleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}