/**
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.datamodeller.client;

import org.kie.workbench.common.screens.datamodeller.client.util.DataModelerUtils;
import org.kie.workbench.common.screens.datamodeller.events.DataModelerEvent;
import org.kie.workbench.common.screens.datamodeller.events.DataModelerPropertyChangeEvent;
import org.kie.workbench.common.screens.datamodeller.model.DataModelTO;
import org.kie.workbench.common.screens.datamodeller.model.DataObjectTO;
import org.kie.workbench.common.screens.datamodeller.model.ObjectPropertyTO;
import org.kie.workbench.common.screens.datamodeller.model.PropertyTypeTO;

import java.util.*;

import static org.kie.workbench.common.screens.datamodeller.client.util.DataModelerUtils.assembleClassName;

public class DataModelHelper {

    private DataModelTO dataModel;

    // Map linking Objects with the Objects they are being referenced by (e.g. x.y.A --> {u.v.B} means B holds a reference to A)
    private Map<String, Set<String>> referencedBy = new HashMap<String, Set<String>>(10);

    // Map linking DataObjects with the Objects they are referencing by (e.g. u.v.B --> {x.y.A} means B references A)
    private Map<String, Set<String>> references = new HashMap<String, Set<String>>(10);

    // Map that keeps track of the offspring a parent class has.
    private Map<String, Set<String>> offspringMap = new HashMap<String, Set<String>>(10);

    // Map of all class names and their corresponding labels (if any) that coexist within a project
    private Map<String, String> classNames = new HashMap<String, String>(10);

    // Map of all labelled class names that coexist within a project
    private Map<String, String> labelledClassNames = new TreeMap<String, String>();

    Map <String, PropertyTypeTO> orderedBaseTypes = new TreeMap<String, PropertyTypeTO>();

    Map <String, PropertyTypeTO> baseTypesByClassName = new HashMap<String, PropertyTypeTO>();

    public DataModelHelper() {
    }

    public Collection<String> getDataObjectReferences( String className ) {
        Collection<String> c = new TreeSet<String>();

        Set<String> s = referencedBy.get( className );
        if ( s != null ) {
            c.addAll( s );
        }

        s = offspringMap.get( className );
        if ( s != null ) {
            c.addAll( s );
        }
        return c;
    }

    public String getObjectLabelByClassName(String className) {
        return classNames.get(className);
    }

    public List<String> getClassList() {
        List<String> l = new ArrayList<String>(classNames.size());
        l.addAll(classNames.keySet());
        return Collections.unmodifiableList(l);
    }

    public Map<String, String> getLabelledClassMap() {
        return Collections.unmodifiableMap(labelledClassNames);
    }

    public Map <String, PropertyTypeTO> getOrderedBaseTypes() {
        return orderedBaseTypes;
    }

    // DataModelHelper methods

    public void dataModelChanged(DataModelerPropertyChangeEvent changeEvent) {
        if (changeEvent.isFrom(dataModel)) {
            if (DataModelerEvent.DATA_OBJECT_EDITOR.equalsIgnoreCase(changeEvent.getSource())) {
                // If any object referenced the object whose name or package just changed, we need to adjust those internally
                if ("name".equals(changeEvent.getPropertyName())) nameChanged( changeEvent.getCurrentDataObject(),
                                                                               (String) changeEvent.getOldValue(),
                                                                               (String) changeEvent.getNewValue());
                else if ("packageName".equals(changeEvent.getPropertyName())) packageChanged( changeEvent.getCurrentDataObject(),
                                                                                              (String) changeEvent.getOldValue(),
                                                                                              (String) changeEvent.getNewValue());
            }
            reset();
        }
    }

    private void nameChanged(DataObjectTO object, String oldName, String newName) {
        adjustDataObjects( assembleClassName(object.getPackageName(), oldName),
                           assembleClassName(object.getPackageName(), newName) );
    }

    private void packageChanged(DataObjectTO object, String oldPackage, String newPackage) {
        adjustDataObjects( assembleClassName(oldPackage, object.getName()),
                           assembleClassName(newPackage, object.getName()) );
    }

    private void adjustDataObjects(String oldClassName, String newClassname) {
        Set<String> s = referencedBy.get(oldClassName);
        if ( s != null && s.size() != 0 ) {
            // Get the object referencing the modified object
            for (String refHolderClassName : s) {
                // Go get the referencing object (in case of a 'self' reference, find it through the new name!)
                DataObjectTO refHolder = dataModel.getDataObjectByClassName(oldClassName.equalsIgnoreCase(refHolderClassName) ? newClassname : refHolderClassName);
                for (ObjectPropertyTO prop : refHolder.getProperties()) {
                    if (oldClassName.equalsIgnoreCase(prop.getClassName())) {
                        prop.setClassName(newClassname);
                    }
                }
            }
        }
        s = offspringMap.get(oldClassName);
        if ( s != null && s.size() != 0 ) {
            for (String offspringClassName : s) {
                DataObjectTO offspring = dataModel.getDataObjectByClassName(offspringClassName);
                offspring.setSuperClassName(newClassname);
            }
        }
    }

    public void dataObjectReferenced(String objectClassName, String subjectClassName) {
        objectReferenced(objectClassName, subjectClassName);
    }

    public void dataObjectUnReferenced(String objectClassName, String subjectClassName) {
        objectUnReferenced(objectClassName, subjectClassName);
    }

    public void dataObjectExtended(String parentClassName, String offspringClassName, Boolean _extends) {
        objectExtended(parentClassName, offspringClassName, _extends);
    }

    public void dataObjectDeleted(String objectClassName) {
        reset();
    }

    public void dataObjectCreated(String objectClassName) {
        reset();
    }

    public void dataObjectSelected(String objectClassName) {
    }

    public void dataObjectUnSelected(String objectClassName) {
    }

    public Boolean isBaseType(String type) {
        return baseTypesByClassName.containsKey(type);
    }

    /**
     * Evaluate if an object can safely extend another one (at least as far as the extension hierarchy is concerned).
     * @param offspringCandidate The class name of the extending object
     * @param parentCandidate The class name of the extended object
     * @return True if the extension does not provoke a conflict with the existing extension hierarchy.
     */
    public Boolean isAssignableFrom(String offspringCandidate, String parentCandidate) {
        if (offspringCandidate == null || offspringCandidate.length() == 0 ||
            parentCandidate == null || parentCandidate.length() == 0 ||
            offspringCandidate.equals(parentCandidate)) return false;

        Set<String> candidatesOffspring = offspringMap.get(offspringCandidate);
        boolean candidateHasOffspring = candidatesOffspring != null && candidatesOffspring.size() > 0;

        if (candidateHasOffspring) {
            if (candidatesOffspring.contains(parentCandidate)) return false;
            for (String newOffspringCandidate : candidatesOffspring) {
                if (!isAssignableFrom(newOffspringCandidate, parentCandidate)) return false;
            }
        }

        return true;
    }

    public void setDataModel(DataModelTO dataModel) {
        this.dataModel = dataModel;
        reset();
    }

    public void setBaseTypes(List<PropertyTypeTO> baseTypes) {
        if (baseTypes != null) {
            for (PropertyTypeTO type : baseTypes) {
                orderedBaseTypes.put(type.getName(), type);
                baseTypesByClassName.put(type.getClassName(), type);
            }
        }
    }

    // Todo can be improved if required for performance reasons
    // Brute force recalculate all
    private void reset() {
        referencedBy.clear();
        references.clear();
        classNames.clear();
        labelledClassNames.clear();
        offspringMap.clear();
        if (dataModel != null) {
            for (DataObjectTO extClassName : dataModel.getExternalClasses()) {
                classNames.put(extClassName.getClassName(), null);
                labelledClassNames.put(extClassName.getClassName(), extClassName.getClassName());
            }
            for (DataObjectTO object : dataModel.getDataObjects()) {
                String className = object.getClassName();
                classNames.put(className, object.getLabel());
                labelledClassNames.put(DataModelerUtils.getDataObjectFullLabel(object), className);

                String superClassName = object.getSuperClassName();
                if (superClassName != null &&  !"".equals(superClassName)) objectExtended(superClassName, className, true);

                for (ObjectPropertyTO prop : object.getProperties()) {
                    if (!prop.isBaseType()) {
                        objectReferenced(prop.getClassName(), object.getClassName());
                    }
                }
            }
        }
    }

    private void objectReferenced(String objectClassName, String subjectClassName) {
        Set<String> refs = referencedBy.get(objectClassName);
        if (refs == null) refs = new HashSet<String>();
        refs.add(subjectClassName);
        referencedBy.put(objectClassName, refs);

        refs = references.get(subjectClassName);
        if (refs == null) refs = new HashSet<String>();
        refs.add(objectClassName);
        references.put(subjectClassName, refs);
    }

    private void objectUnReferenced(String objectClassName, String subjectClassName) {
        Set<String> refs = referencedBy.get(objectClassName);
        if (refs != null && refs.size() > 0) {
            refs.remove(subjectClassName);
        }
//        else ("Error de-referencing data object (referenced object)."));

        refs = references.get(subjectClassName);
        if (refs != null && refs.size() > 0) {
            refs.remove(objectClassName);
        }
//        else ("Error de-referencing data object (referring object)."));
    }

    private void objectExtended(String parentClassName, String offspringClassName, Boolean _extends) {
        Set<String> _offspring = offspringMap.get(parentClassName);

        if (_extends) {
            if (_offspring != null ) _offspring.add(offspringClassName);
            else {
                _offspring = new HashSet<String>();
                _offspring.add(offspringClassName);
                offspringMap.put(parentClassName, _offspring);
            }
        } else {
            if (_offspring != null && _offspring.size() > 0) _offspring.remove(offspringClassName);
            if (_offspring.size() == 0) offspringMap.remove(parentClassName);
//            else ("Superclass referencing error"));
        }
    }
}
