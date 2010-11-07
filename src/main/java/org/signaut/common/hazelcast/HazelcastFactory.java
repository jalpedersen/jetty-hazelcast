/*
Copyright (c) 2010, Jesper André Lyngesen Pedersen
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
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
        throw new IllegalStateException("Failed to load hazelcast configuration from " + filename);
    }

}
