/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.model;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple business object representing a pet.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Entity
@Table(name = "pets")
public class Pet extends NamedEntity {

    @Column(name = "birth_date")
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private PetType type;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER)
    private Set<Visit> visits;

    @Column(name = "nickname")
    @ElementCollection
    @CollectionTable(name = "nicknames", joinColumns = @JoinColumn(name = "pet_id"))
    private List<String> nicknames;

    public void addNicknameVisit(String nickname) {
        getNicknamesInternal().add(nickname);
    }

    public void addVisit(Visit visit) {
        getVisitsInternal().add(visit);
        visit.setPet(this);
    }

    public Date getBirthDate() {
        return this.birthDate;
    }

    public List<String> getNicknames() {
        return Collections.unmodifiableList(new ArrayList<String>(getNicknamesInternal()));
    }

    protected List<String> getNicknamesInternal() {
        if (this.nicknames == null) {
            this.nicknames = new ArrayList<String>();
        }
        return this.nicknames;
    }

    public PetType getType() {
        return this.type;
    }

    public Owner getOwner() {
        return this.owner;
    }

    public List<Visit> getVisits() {
        return getVisitsInternal().stream()
                .sorted(Comparator.comparing(Visit::getDate))
                .collect(Collectors.toUnmodifiableList());
    }

    protected Set<Visit> getVisitsInternal() {
        if (this.visits == null) {
            this.visits = new HashSet<Visit>();
        }
        return this.visits;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames;
    }

    protected void setOwner(Owner owner) {
        this.owner = owner;
    }

    public void setType(PetType type) {
        this.type = type;
    }

    protected void setVisitsInternal(Set<Visit> visits) {
        this.visits = visits;
    }

}
