package com.crittercare.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The Zoo is the in-memory aggregate root.
 *
 * It holds the live collections of Animals, Enclosures, and Alerts that
 * the SimulationEngine ticks on every cycle.  It is NOT persisted directly
 * — the repositories read from / write to the H2 database; the Zoo is
 * re-hydrated from the DB at startup.
 *
 * Why an aggregate root?
 *  Keeping everything in one place lets the SimulationEngine iterate all
 *  animals and enclosures in a single tick without making multiple service
 *  calls.  Services remain the authoritative owners of business rules;
 *  Zoo is just a snapshot of the live state.
 */
public class Zoo {

    private String          name;
    private List<Animal>    animals;
    private List<Enclosure> enclosures;
    private List<Alert>     activeAlerts;

    // ── Constructor ──────────────────────────────────────────────────────────

    public Zoo(String name) {
        this.name         = name;
        this.animals      = new ArrayList<>();
        this.enclosures   = new ArrayList<>();
        this.activeAlerts = new ArrayList<>();
    }

    // ── Animal management ────────────────────────────────────────────────────

    public void addAnimal(Animal animal) {
        animals.add(animal);
    }

    public boolean removeAnimal(int id) {
        return animals.removeIf(a -> a.getId() == id);
    }

    public Optional<Animal> getAnimalById(int id) {
        return animals.stream().filter(a -> a.getId() == id).findFirst();
    }

    public List<Animal> getAnimalsInEnclosure(int enclosureId) {
        return animals.stream()
                .filter(a -> a.getEnclosureId() == enclosureId)
                .toList();
    }

    // ── Enclosure management ─────────────────────────────────────────────────

    public void addEnclosure(Enclosure enclosure) {
        enclosures.add(enclosure);
    }

    public boolean removeEnclosure(int id) {
        return enclosures.removeIf(e -> e.getId() == id);
    }

    public Optional<Enclosure> getEnclosureById(int id) {
        return enclosures.stream().filter(e -> e.getId() == id).findFirst();
    }

    // ── Alert management ─────────────────────────────────────────────────────

    public void addAlert(Alert alert) {
        activeAlerts.add(alert);
    }

    public void resolveAlert(int alertId) {
        activeAlerts.stream()
                .filter(a -> a.getId() == alertId)
                .findFirst()
                .ifPresent(Alert::resolve);
    }

    public void clearResolvedAlerts() {
        activeAlerts.removeIf(Alert::isResolved);
    }

    // ── Summary statistics ───────────────────────────────────────────────────

    public int getTotalAnimals() {
        return animals.size();
    }

    public int getTotalEnclosures() {
        return enclosures.size();
    }

    public long getCriticalAlertCount() {
        return activeAlerts.stream()
                .filter(a -> !a.isResolved()
                        && a.getSeverity() == AlertSeverity.CRITICAL)
                .count();
    }

    public long getAnimalsRequiringAttentionCount() {
        return animals.stream()
                .filter(Animal::requiresAttention)
                .count();
    }

    public long getEnclosuresNeedingCleaningCount() {
        return enclosures.stream()
                .filter(Enclosure::isCleaningDue)
                .count();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }

    public List<Animal> getAnimals()                    { return Collections.unmodifiableList(animals); }
    public void setAnimals(List<Animal> animals)        { this.animals = new ArrayList<>(animals); }

    public List<Enclosure> getEnclosures()              { return Collections.unmodifiableList(enclosures); }
    public void setEnclosures(List<Enclosure> list)     { this.enclosures = new ArrayList<>(list); }

    public List<Alert> getActiveAlerts()                { return Collections.unmodifiableList(activeAlerts); }
    public void setActiveAlerts(List<Alert> alerts)     { this.activeAlerts = new ArrayList<>(alerts); }
}
