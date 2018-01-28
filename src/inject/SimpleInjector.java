package inject;

public interface SimpleInjector {
    SimpleInjector target(Object container);
    SimpleInjector with(Class<?> target, Object... param);
    void inject();

    static  SimpleInjector of(Class<?> container) {
        return new SimpleInjectorImp(container);
    }
}
