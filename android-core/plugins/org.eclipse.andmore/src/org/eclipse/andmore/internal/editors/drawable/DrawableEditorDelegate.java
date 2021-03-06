/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.editors.drawable;

import static org.eclipse.andmore.AdtConstants.EDITORS_NAMESPACE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Editor for /res/drawable XML files.
 */
@SuppressWarnings("restriction")
public class DrawableEditorDelegate extends CommonXmlDelegate {

    public static class Creator implements IDelegateCreator {
        @Override
        @SuppressWarnings("unchecked")
        public DrawableEditorDelegate createForFile(
                @NonNull CommonXmlEditor delegator,
                @Nullable ResourceFolderType type) {
            if (ResourceFolderType.DRAWABLE == type) {
                return new DrawableEditorDelegate(delegator);
            }

            return null;
        }
    }

    /**
     * Old standalone-editor ID.
     * Use {@link CommonXmlEditor#ID} instead.
     */
    public static final String LEGACY_EDITOR_ID =
        EDITORS_NAMESPACE + ".drawable.DrawableEditor"; //$NON-NLS-1$

    /** The tag used at the root */
    private String mRootTag;

    /**
     * Creates the form editor for resources XML files.
     */
    private DrawableEditorDelegate(CommonXmlEditor editor) {
        super(editor, new DrawableContentAssist());
        editor.addDefaultTargetListener();
    }

    @Override
    public void delegateCreateFormPages() {
        /* Disabled for now; doesn't work quite right
        try {
            addPage(new DrawableTreePage(this));
        } catch (PartInitException e) {
            AdtPlugin.log(IStatus.ERROR, "Error creating nested page"); //$NON-NLS-1$
            AdtPlugin.getDefault().getLog().log(e.getStatus());
        }
        */
     }

    @Override
    public void delegateXmlModelChanged(Document xmlDoc) {
        Element rootElement = xmlDoc.getDocumentElement();
        if (rootElement != null) {
            mRootTag = rootElement.getTagName();
        }

        delegateInitUiRootNode(false /*force*/);

        if (mRootTag != null
                && !mRootTag.equals(getUiRootNode().getDescriptor().getXmlLocalName())) {
            AndroidTargetData data = getEditor().getTargetData();
            if (data != null) {
                ElementDescriptor descriptor =
                    data.getDrawableDescriptors().getElementDescriptor(mRootTag);
                // Replace top level node now that we know the actual type

                // Disconnect from old
                getUiRootNode().setEditor(null);
                getUiRootNode().setXmlDocument(null);

                // Create new
                setUiRootNode(descriptor.createUiNode());
                getUiRootNode().setXmlDocument(xmlDoc);
                getUiRootNode().setEditor(getEditor());
            }
        }

        if (getUiRootNode().getDescriptor() instanceof DocumentDescriptor) {
            getUiRootNode().loadFromXmlNode(xmlDoc);
        } else {
            getUiRootNode().loadFromXmlNode(rootElement);
        }
    }

    @Override
    public void delegateInitUiRootNode(boolean force) {
        // The manifest UI node is always created, even if there's no corresponding XML node.
        if (getUiRootNode() == null || force) {
            ElementDescriptor descriptor;
            boolean reload = false;
            AndroidTargetData data = getEditor().getTargetData();
            if (data == null) {
                descriptor = new DocumentDescriptor("temp", null /*children*/);
            } else {
                descriptor = data.getDrawableDescriptors().getElementDescriptor(mRootTag);
                reload = true;
            }
            setUiRootNode(descriptor.createUiNode());
            getUiRootNode().setEditor(getEditor());

            if (reload) {
                onDescriptorsChanged();
            }
        }
    }

    private void onDescriptorsChanged() {
        IStructuredModel model = getEditor().getModelForRead();
        if (model != null) {
            try {
                Node node = getEditor().getXmlDocument(model).getDocumentElement();
                getUiRootNode().reloadFromXmlNode(node);
            } finally {
                model.releaseFromRead();
            }
        }
    }
}
