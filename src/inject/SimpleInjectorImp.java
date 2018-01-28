package inject;

import inject.annotations.Inject;
import inject.annotations.ModuleContainer;
import inject.annotations.ModuleTarget;
import inject.annotations.Provide;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class SimpleInjectorImp implements SimpleInjector {
    private final Class<?> containerClass;
    private Object target = null;
    private Object injectionModule = null;
    private Map<Class<?>, Object[]> params = null;

    SimpleInjectorImp(Class<?> container) {
        final ModuleContainer[] mc = container.getAnnotationsByType(ModuleContainer.class);
        if (mc == null || mc.length == 0) {
            throw new IllegalArgumentException("Class is not annotated by ModuleContainer");
        }
        this.containerClass = container;
    }

    @Override
    public SimpleInjector target(Object target) {
        this.target = target;

        for (Method m : this.containerClass.getDeclaredMethods()) {
            final ModuleTarget[] moduleTarget = m.getAnnotationsByType(ModuleTarget.class);
            if (moduleTarget != null && moduleTarget.length > 0) {
                final Class targetClass = moduleTarget[0].targetClass();
                if (this.target.getClass() == targetClass) {
                    try {
                        this.injectionModule = m.invoke(containerClass.newInstance(), (Object[])null);
                        return this;
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        throw new IllegalStateException("don't match targetClass! Check ContainerClass define");
    }

    @Override
    public SimpleInjector with(Class<?> target, Object... params) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }
        this.params.put(target, params);
        return this;
    }

    @Override
    public void inject() {
        if (this.target == null) throw new IllegalStateException("Target is not set!");
        if (this.injectionModule == null) throw new IllegalStateException("Injection Module is not set!");

        final Field[] injectedField = this.target.getClass().getDeclaredFields();
        final Method[] providers = injectionModule.getClass().getDeclaredMethods();

        for (Method provider : providers) {
            final Provide[] provides = provider.getAnnotationsByType(Provide.class);
            if (provides != null && provides.length > 0) {
                final Class provideClass = provides[0].provideClass();

                for (Field f : injectedField) {
                    f.setAccessible(true);
                    final Inject[] injections = f.getAnnotationsByType(Inject.class);
                    if (injections != null && injections.length > 0 && f.getType() == provideClass) {
                        try {
                            if (provider.getParameterCount() > 0) {
                                if (this.params == null || !this.params.containsKey(provideClass)) throw new IllegalStateException("Provider has one more parameter but not call with method");
                                if (provider.getParameterCount() != this.params.get(provideClass).length) {
                                    throw new IllegalArgumentException("Params count does not match!");
                                }
                                f.set(this.target, provider.invoke(this.injectionModule, this.params.get(provideClass)));
                            } else {
                                f.set(this.target, provider.invoke(this.injectionModule, (Object[])null));
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
