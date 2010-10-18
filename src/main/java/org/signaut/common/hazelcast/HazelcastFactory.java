package org.signaut.common.hazelcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastFactory {
    /**
     * Loads a file relative to the current location - if this fails it will
     * load a resource from the class-loader of <code>loadingClass</code>
     * 
     * @param filename
     * @param loadingClass
     * @return a new HazelcastInstance based on the configuration in
     *         <code>filename</code>
     * @throws IllegalStateException
     *             if no file is found
     */
    public HazelcastInstance loadHazelcastInstance(String filename, Class<?> loadingClass) {
        // Load file from current location
        final File file = new File("./" + filename);
        final XmlConfigBuilder configBuilder;
        if (file.exists()) {
            try {
                configBuilder = new XmlConfigBuilder(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Failed to load file " + filename, e);
            }
            return Hazelcast.newHazelcastInstance(configBuilder.build());
        } else {
            // If this fails, load from classpath
            final InputStream resource = loadingClass.getResourceAsStream(filename);
            if (resource != null) {
                configBuilder = new XmlConfigBuilder(resource);
                return Hazelcast.newHazelcastInstance(configBuilder.build());
            }
        }
        // Bail out if all else fails
        throw new IllegalStateException("Failed to load hazelcast configuration from " + file);
    }

}
