/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.model.javasyntax.type;

import java.util.Map;

public record WildcardSuperTypeArgument(Type type) implements TypeArgument {

    @Override
    public boolean isTypeArgumentAssignableFrom(Map<String, BaseType> typeBounds, BaseTypeArgument typeArgument) {
        if (typeArgument.isWildcardSuperTypeArgument()) {
            return type.isTypeArgumentAssignableFrom(typeBounds, typeArgument.type());
        }
		if (typeArgument instanceof Type) {
            return type.isTypeArgumentAssignableFrom(typeBounds, typeArgument);
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
			return true;
		}
        if (o == null || getClass() != o.getClass()) {
			return false;
		}

        WildcardSuperTypeArgument that = (WildcardSuperTypeArgument) o;

        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        return 979510081 + (type != null ? type.hashCode() : 0);
    }

    @Override
    public boolean isWildcardSuperTypeArgument() { return true; }

    @Override
    public void accept(TypeArgumentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "WildcardSuperTypeArgument{? super " + type + "}";
    }
}
