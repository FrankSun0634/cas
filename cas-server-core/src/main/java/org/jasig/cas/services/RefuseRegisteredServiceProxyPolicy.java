/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.services;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URL;

/**
 * A proxy policy that disallows proxying.
 * @author Misagh Moayyed
 * @since 4.1
 */
public final class RefuseRegisteredServiceProxyPolicy implements RegisteredServiceProxyPolicy {

    private static final long serialVersionUID = -5718445151129901484L;

    @Override
    public boolean isAllowedToProxy() {
        return false;
    }

    @Override
    public boolean isAllowedProxyCallbackUrl(final URL pgtUrl) {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof RegisteredServiceProxyPolicy)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder bldr = new HashCodeBuilder(13, 133);
        return bldr.appendSuper(this.hashCode()).toHashCode();
    }
}
