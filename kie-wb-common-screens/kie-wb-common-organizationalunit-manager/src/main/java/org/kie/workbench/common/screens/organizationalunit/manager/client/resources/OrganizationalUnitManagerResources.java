/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.organizationalunit.manager.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import org.kie.workbench.common.screens.organizationalunit.manager.client.resources.css.OrganizationalUnitManagerCss;

/**
 * Resources for the Organizational Manager editor
 */
public interface OrganizationalUnitManagerResources extends ClientBundle {

    public static final OrganizationalUnitManagerResources INSTANCE = GWT.create( OrganizationalUnitManagerResources.class );

    @Source("css/styles.css")
    OrganizationalUnitManagerCss CSS();

}
