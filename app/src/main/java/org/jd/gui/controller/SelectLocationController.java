/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.controller;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.gui.api.API;
import org.jd.gui.api.model.Container;
import org.jd.gui.api.model.Container.Entry;
import org.jd.gui.api.model.Type;
import org.jd.gui.service.type.TypeFactoryService;
import org.jd.gui.spi.TypeFactory;
import org.jd.gui.util.ImageUtil;
import org.jdv1.gui.model.container.DelegatingFilterContainer;
import org.jdv1.gui.view.SelectLocationView;
import org.netbeans.modules.editor.java.JavaKit;

import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

import de.cismet.custom.visualdiff.DiffPanel;

public class SelectLocationController {

    private final API api;
    @SuppressWarnings("all")
    private final SelectLocationView selectLocationView;

    public SelectLocationController(API api, JFrame mainFrame) {
        this.api = api;
        // Create UI
        selectLocationView = new SelectLocationView<>(api, mainFrame);
    }

    @SuppressWarnings("unchecked")
    public void show(Point location, Collection<Container.Entry> entries, Consumer<Container.Entry> selectedLocationCallback, Runnable closeCallback) {
        // Show UI
        Map<Container, List<Container.Entry>> map = new HashMap<>();

        for (Container.Entry entry : entries) {
            Container container = entry.getContainer();

            // Search root container
            while (true) {
                Container parentContainer = container.getRoot().getParent().getContainer();
                if (parentContainer.getRoot() == null) {
                    break;
                }
                container = parentContainer;
            }

            map.computeIfAbsent(container, c -> new ArrayList<>()).add(entry);
        }

        Set<DelegatingFilterContainer> delegatingFilterContainers = new HashSet<>();

        for (Map.Entry<Container, List<Container.Entry>> mapEntry : map.entrySet()) {
            Container container = mapEntry.getKey();
            // Create a filtered container
            // TODO In a future release, display matching types and inner-types, not only matching files
            delegatingFilterContainers.add(new DelegatingFilterContainer(container, getOuterEntries(mapEntry.getValue())));
        }

        Consumer<URI> selectedEntryCallback = uri -> onLocationSelected(delegatingFilterContainers, uri, selectedLocationCallback);

        Entry[] pair = entries.toArray(Entry[]::new);
        String fileNameLeft = new File(pair[0].getContainer().getRoot().getParent().getUri()).getName();
        String fileNameRight = new File(pair[1].getContainer().getRoot().getParent().getUri()).getName();
        String className = new File(pair[0].getUri()).getName();
        ImageIcon icon = new ImageIcon(ImageUtil.getImage("/org/jd/gui/images/jd_icon_64.png"));
        String message = "Compare class " + className + " in files " + fileNameLeft + " and " + fileNameRight + "?";
        if (entries.size() == 2 && JOptionPane.showConfirmDialog(null, message, "Compare JAVA source files ?", YES_NO_OPTION, QUESTION_MESSAGE, icon) == YES_OPTION) {
            JFrame diffFrame = new JFrame("Comparison view for class " + className);
            ImageUtil.addJDIconsToFrame(diffFrame);
            DiffPanel diffPanel = new DiffPanel(diffFrame);
            try (InputStream left = pair[0].getInputStream(); InputStream right = pair[1].getInputStream()) {
                String contentLeft = IOUtils.toString(left, StandardCharsets.UTF_8);
                String contentRight = IOUtils.toString(right, StandardCharsets.UTF_8);
                diffPanel.setLeftAndRight(contentLeft, JavaKit.JAVA_MIME_TYPE, fileNameLeft, contentRight, JavaKit.JAVA_MIME_TYPE, fileNameRight);
                diffFrame.getContentPane().add(diffPanel);
                diffFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
                diffFrame.setVisible(true);
            } catch (Exception e) {
                ExceptionUtil.printStackTrace(e);
            }
        } else {
            selectLocationView.show(location, delegatingFilterContainers, entries.size(), selectedEntryCallback, closeCallback);
        }
    }

    protected Collection<Container.Entry> getOuterEntries(Collection<Container.Entry> entries) {
        Map<Container.Entry, Container.Entry> innerTypeEntryToOuterTypeEntry = new HashMap<>();
        Set<Container.Entry> outerEntriesSet = new HashSet<>();

        for (Container.Entry entry : entries) {
            Container.Entry outerTypeEntry = null;
            TypeFactory factory = TypeFactoryService.getInstance().get(entry);

            if (factory != null) {
                Type type = factory.make(api, entry, null);

                if (type != null && type.getOuterName() != null) {
                    outerTypeEntry = innerTypeEntryToOuterTypeEntry.get(entry);

                    if (outerTypeEntry == null) {
                        Map<String, Container.Entry> typeNameToEntry = new HashMap<>();
                        Map<String, String> innerTypeNameToOuterTypeName = new HashMap<>();

                        // Populate "typeNameToEntry" and "innerTypeNameToOuterTypeName"
                        for (Container.Entry e : entry.getParent().getChildren().values()) {
                            factory = TypeFactoryService.getInstance().get(e);

                            if (factory != null) {
                                type = factory.make(api, e, null);

                                if (type != null) {
                                    typeNameToEntry.put(type.getName(), e);
                                    if (type.getOuterName() != null) {
                                        innerTypeNameToOuterTypeName.put(type.getName(), type.getOuterName());
                                    }
                                }
                            }
                        }

                        // Search outer type entries and populate "innerTypeEntryToOuterTypeEntry"
                        for (Map.Entry<String, String> e : innerTypeNameToOuterTypeName.entrySet()) {
                            Container.Entry innerTypeEntry = typeNameToEntry.get(e.getKey());

                            if (innerTypeEntry != null) {
                                String outerTypeName = e.getValue();

                                for (;;) {
                                    String typeName = innerTypeNameToOuterTypeName.get(outerTypeName);
                                    if (typeName == null) {
                                        break;
                                    }
                                    outerTypeName = typeName;
                                }

                                outerTypeEntry = typeNameToEntry.get(outerTypeName);

                                if (outerTypeEntry != null) {
                                    innerTypeEntryToOuterTypeEntry.put(innerTypeEntry, outerTypeEntry);
                                }
                            }
                        }

                        // Get outer type entry
                        outerTypeEntry = innerTypeEntryToOuterTypeEntry.get(entry);
                    }
                }
            }

            if (outerTypeEntry != null) {
                outerEntriesSet.add(outerTypeEntry);
            } else {
                outerEntriesSet.add(entry);
            }
        }

        // Return outer type entries sorted by path
        List<Container.Entry> result = new ArrayList<>(outerEntriesSet);

        result.sort(Comparator.comparing(Entry::getPath));

        return result;
    }

    protected void onLocationSelected(Set<DelegatingFilterContainer> delegatingFilterContainers, URI uri, Consumer<Container.Entry> selectedLocationCallback) {
        // Open the single entry uri
        Container.Entry entry = null;

        for (DelegatingFilterContainer container : delegatingFilterContainers) {
            entry = container.getEntry(uri);
            if (entry != null) {
                break;
            }
        }

        if (entry != null) {
            selectedLocationCallback.accept(entry);
        }
    }

}
