package datawave.data.type;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * TypeFactory that uses an internal loading cache to limit new Type objects
 */
public class TypeFactory {
    
    //  @formatter:off
    private final LoadingCache<String, Type<?>> typeCache = CacheBuilder.newBuilder()
                    .maximumSize(128)
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .build(new CacheLoader<>() {
                        @Override public Type<?> load(String className) throws Exception {
                            Class<?> clazz = Class.forName(className);
                            return (Type<?>) clazz.getDeclaredConstructor().newInstance();
                        }
                    });
    //  @formatter:on
    
    public TypeFactory() {
        // empty constructor
    }
    
    /**
     * Create a {@link Type} for the given class name
     * 
     * @param className
     *            the class name
     * @return the Type
     */
    public Type<?> createType(String className) {
        try {
            return typeCache.get(className);
        } catch (Exception e) {
            throw new IllegalStateException("Error creating instance of class " + className);
        }
    }
}
