package de.bitgrip.ficum.visitor;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Restaurant implements Serializable {

    private Address address;

    private String borough;

    private String cuisine;

    private Grade grade;

    private String name;

    private Long id;

    public Address getAddress() {
        return address;
    }

    public String getBorough() {
        return borough;
    }

    public String getCuisine() {
        return cuisine;
    }

    public Grade getGrade() {
        return grade;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setBorough(String borough) {
        this.borough = borough;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public void setGrades(Grade[] grades) {
        setGrade(grades[0]);
    }

    @JsonSetter("restaurant_id")
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class Address implements Serializable {
        private String building;
        private String street;
        private String zipcode;
        private Location location;

        public String getBuilding() {
            return building;
        }

        public Location getLocation() {
            return location;
        }

        public String getStreet() {
            return street;
        }

        public String getZipcode() {
            return zipcode;
        }

        public void setBuilding(String building) {
            this.building = building;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public void setZipcode(String zipcode) {
            this.zipcode = zipcode;
        }
    }

    public static class Grade implements Serializable {
        private String grade;

        @JsonDeserialize(using = LongToDateDeserializer.class)
        private Date date;

        private Integer score;

        public Date getDate() {
            return date;
        }

        public String getGrade() {
            return grade;
        }

        public Integer getScore() {
            return score;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }

    public static class Location implements Serializable {
        private String type;
        private Long[] coordinates;

        public Long[] getCoordinates() {
            return coordinates;
        }

        public String getType() {
            return type;
        }

        public void setCoordinates(Long[] coordinates) {
            this.coordinates = coordinates;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

}
