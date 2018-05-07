/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author pmikova
 */
public class VmId {
    private String id;

    public VmId() {
        id = UUID.randomUUID().toString();
    }

    public VmId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VmId other = (VmId) o;
        return Objects.equals(this.id, other.get());
    }

    public String get() {
        return id;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}