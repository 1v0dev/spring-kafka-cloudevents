package com.dev1v0.kafka;

import com.github.javafaker.Faker;

public class SampleData {

    private static final Faker faker = new Faker();
    private String name;

    private int age;

    private String company;

    public SampleData() {
        name = faker.name().fullName();
        age = faker.number().numberBetween(18, 70);
        company = faker.company().name();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    @Override
    public String toString() {
        return "SampleData{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", company='" + company + '\'' +
                '}';
    }
}
