/*
 * Copyright 2016, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.sync.api;

import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.zanata.sync.common.exception.RepoSyncExMapper;
import org.zanata.sync.common.exception.ZanataSyncExMapper;
import com.google.common.collect.ImmutableSet;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@ApplicationPath("api")
public class JobApplication extends Application {
    private Set<Class<?>> classes = buildClasses();

    private static Set<Class<?>> buildClasses() {
        return ImmutableSet.<Class<?>>builder()
                // api classes
                .add(JobResource.class)
                // providers
                .add(ZanataSyncExMapper.class)
                .add(RepoSyncExMapper.class)
                .build();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
