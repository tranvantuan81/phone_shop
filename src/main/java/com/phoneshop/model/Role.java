package com.phoneshop.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String description;

    // role - one => many - users . ctrl + k . press 's'
    @OneToMany(mappedBy = "role")
    private List<User> users;

    @Override
    public String toString() {
        return "Role [id=" + id + ", name=" + name + ", description=" + description + "]";
    }

}
