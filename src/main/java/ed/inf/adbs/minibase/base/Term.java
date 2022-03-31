package ed.inf.adbs.minibase.base;

//  a basic implementation of Term taht contains an override of object equality, which is expanded upon in the different subclasses
public class Term {
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object != null && getClass() == object.getClass();
    }
}


