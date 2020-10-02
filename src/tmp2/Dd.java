package tmp2;

class GenericFactory<T> {
    Class theClass = null;

    public GenericFactory(Class theClass) {
        this.theClass = theClass;
    }

    public T createInstance()
            throws IllegalAccessException, InstantiationException {
        return (T) this.theClass.newInstance();
    }
}

public class Dd {
}
