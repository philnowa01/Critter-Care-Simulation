package com.crittercare.service;

import com.crittercare.model.Zookeeper;
import com.crittercare.repository.ZookeeperRepository;

import java.util.List;

public class ZookeeperService {

    private final ZookeeperRepository repo;
    private Zookeeper current;

    public ZookeeperService(ZookeeperRepository repo) {
        this.repo = repo;
        List<Zookeeper> all = repo.findAll();
        this.current = all.isEmpty() ? null : all.get(0);
    }

    public List<Zookeeper> getAll()            { return repo.findAll(); }
    public Zookeeper       getCurrent()        { return current; }
    public void            setCurrent(Zookeeper z) { this.current = z; }

    public Zookeeper add(String name) {
        Zookeeper z = repo.save(name.trim());
        if (current == null) current = z;
        return z;
    }

    public boolean rename(int id, String newName) {
        boolean ok = repo.rename(id, newName.trim());
        if (ok && current != null && current.getId() == id) {
            current.setName(newName.trim());
        }
        return ok;
    }

    public boolean delete(int id) {
        return repo.delete(id);
    }
}
