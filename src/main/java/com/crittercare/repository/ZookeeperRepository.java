package com.crittercare.repository;

import com.crittercare.model.Zookeeper;
import java.util.List;

public interface ZookeeperRepository {
    List<Zookeeper> findAll();
    Zookeeper save(String name);
    boolean rename(int id, String newName);
    boolean delete(int id);
}
