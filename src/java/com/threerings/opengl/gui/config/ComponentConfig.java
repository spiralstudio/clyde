//
// $Id$

package com.threerings.opengl.gui.config;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;
import com.threerings.util.MessageBundle;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Label.Fit;
import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.config.LayoutConfig.Justification;
import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.util.GlContext;

/**
 * Contains a component configuration.
 */
@EditorTypes({
    ComponentConfig.Button.class, ComponentConfig.CheckBox.class,
    ComponentConfig.ComboBox.class, ComponentConfig.Container.class,
    ComponentConfig.HTMLView.class, ComponentConfig.Label.class,
    ComponentConfig.List.class, ComponentConfig.PasswordField.class,
    ComponentConfig.ScrollBar.class, ComponentConfig.ScrollPane.class,
    ComponentConfig.Slider.class, ComponentConfig.Spacer.class,
    ComponentConfig.TabbedPane.class, ComponentConfig.TextArea.class,
    ComponentConfig.TextField.class, ComponentConfig.ToggleButton.class,
    ComponentConfig.UserInterface.class })
public abstract class ComponentConfig extends DeepObject
    implements Exportable
{
    /** Available label orientations. */
    public enum Orientation
    {
        HORIZONTAL(UIConstants.HORIZONTAL),
        VERTICAL(UIConstants.VERTICAL);

        /**
         * Returns the corresponding UI constant.
         */
        public int getConstant ()
        {
            return _constant;
        }

        Orientation (int constant)
        {
            _constant = constant;
        }

        /** The corresponding UI constant. */
        protected int _constant;
    }

    /**
     * A label.
     */
    public static class Label extends ComponentConfig
    {
        /** The label's icon, if any. */
        @Editable(nullable=true)
        public IconConfig icon;

        /** The label's text. */
        @Editable(hgroup="t")
        public String text = "";

        /** The gap between icon and text. */
        @Editable(hgroup="t")
        public int iconTextGap = 3;

        /** The label orientation. */
        @Editable(hgroup="o")
        public Orientation orientation = Orientation.HORIZONTAL;

        /** Determines how to fit overlong text in the label. */
        @Editable(hgroup="o")
        public Fit fit = Fit.WRAP;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Label.class) ?
                comp : new com.threerings.opengl.gui.Label(ctx, "");
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.Label label = (com.threerings.opengl.gui.Label)comp;
            label.setIcon(icon == null ? null : icon.getIcon(ctx));
            label.setText(getMessage(msgs, text));
            label.setIconTextGap(iconTextGap);
            label.setOrientation(orientation.getConstant());
            label.setFit(fit);
        }
    }

    /**
     * A button.
     */
    public static class Button extends Label
    {
        /** The action to fire when the button is pressed. */
        @Editable(hgroup="a")
        public String action = "";

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Button.class) ?
                comp : new com.threerings.opengl.gui.Button(ctx, "");
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.Button)comp).setAction(action);
        }
    }

    /**
     * A toggle button.
     */
    public static class ToggleButton extends Button
    {
        /** Whether or not the button is selected. */
        @Editable(hgroup="a")
        public boolean selected;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ToggleButton.class) ?
                comp : new com.threerings.opengl.gui.ToggleButton(ctx, "");
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.ToggleButton)comp).setSelected(selected);
        }
    }

    /**
     * A check box.
     */
    public static class CheckBox extends ToggleButton
    {
        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.CheckBox.class) ?
                comp : new com.threerings.opengl.gui.CheckBox(ctx, "");
        }
    }

    /**
     * A combo box.
     */
    public static class ComboBox extends ComponentConfig
    {
        /**
         * A single item in the list.
         */
        @EditorTypes({ StringItem.class, IconItem.class })
        public static abstract class Item extends DeepObject
            implements Exportable
        {
            /**
             * Returns the object corresponding to this item.
             */
            public abstract Object getObject (GlContext ctx, MessageBundle msgs);
        }

        /**
         * A string item.
         */
        public static class StringItem extends Item
        {
            /** The text of the item. */
            @Editable
            public String text = "";

            @Override // documentation inherited
            public Object getObject (GlContext ctx, MessageBundle msgs)
            {
                return getMessage(msgs, text);
            }
        }

        /**
         * An icon item.
         */
        public static class IconItem extends Item
        {
            /** The item icon. */
            @Editable(nullable=true)
            public IconConfig icon;

            @Override // documentation inherited
            public Object getObject (GlContext ctx, MessageBundle msgs)
            {
                return (icon == null) ? null : icon.getIcon(ctx);
            }
        }

        /** The items available for selection. */
        @Editable
        public Item[] items = new Item[0];

        /** The index of the selected item. */
        @Editable(min=0)
        public int selected;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.ComboBox.class) ?
                comp : new com.threerings.opengl.gui.ComboBox(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.ComboBox box = (com.threerings.opengl.gui.ComboBox)comp;
            Object[] objects = new Object[items.length];
            for (int ii = 0; ii < items.length; ii++) {
                objects[ii] = items[ii].getObject(ctx, msgs);
            }
            box.setItems(objects);
            box.selectItem(selected);
        }
    }

    /**
     * A list.
     */
    public static class List extends ComponentConfig
    {
        /** The items available for selection. */
        @Editable
        public String[] items = new String[0];

        /** The index of the selected item. */
        @Editable(min=0)
        public int selected;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.List.class) ?
                comp : new com.threerings.opengl.gui.List(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.List list = (com.threerings.opengl.gui.List)comp;
            Object[] values = new Object[items.length];
            for (int ii = 0; ii < items.length; ii++) {
                values[ii] = getMessage(msgs, items[ii]);
            }
            list.setValues(values);
            list.setSelectedValue(selected < values.length ? values[selected] : null);
        }
    }

    /**
     * Base class for text components.
     */
    public static abstract class TextComponent extends ComponentConfig
    {
        /** The text in the component. */
        @Editable(hgroup="t")
        public String text = "";

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            ((com.threerings.opengl.gui.TextComponent)comp).setText(getMessage(msgs, text));
        }
    }

    /**
     * A text field.
     */
    public static class TextField extends TextComponent
    {
        /** The maximum length of the field (or zero for unlimited). */
        @Editable(min=0, hgroup="t")
        public int maxLength;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextField.class) ?
                comp : new com.threerings.opengl.gui.TextField(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
             // setMaxLength must be called before setText
            ((com.threerings.opengl.gui.TextField)comp).setMaxLength(maxLength);
            super.configure(ctx, scope, msgs, comp);
        }
    }

    /**
     * A password field.
     */
    public static class PasswordField extends TextField
    {
        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.PasswordField.class) ?
                comp : new com.threerings.opengl.gui.PasswordField(ctx);
        }
    }

    /**
     * A text area.
     */
    public static class TextArea extends ComponentConfig
    {
        /** The text in the component. */
        @Editable(hgroup="t")
        public String text = "";

        /** The area's preferred width, or zero for none. */
        @Editable(min=0, hgroup="t")
        public int preferredWidth;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TextArea.class) ?
                comp : new com.threerings.opengl.gui.TextArea(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.TextArea area = (com.threerings.opengl.gui.TextArea)comp;
            area.setPreferredWidth(preferredWidth);
            area.setText(getMessage(msgs, text));
        }
    }

    /**
     * A tabbed pane.
     */
    public static class TabbedPane extends ComponentConfig
    {
        /**
         * A single tab.
         */
        public static class Tab extends DeepObject
            implements Exportable
        {
            /** The tab title. */
            @Editable(hgroup="t")
            public String title = "";

            /** Whether or not the tab has a close button. */
            @Editable(hgroup="t")
            public boolean hasClose;

            /** The tab component. */
            @Editable
            public ComponentConfig component = new ComponentConfig.Spacer();
        }

        /** The tab alignment. */
        @Editable(hgroup="t")
        public Justification tabAlignment = Justification.LEFT;

        /** The tab gap. */
        @Editable(hgroup="t")
        public int gap = GroupLayout.DEFAULT_GAP;

        /** The tabs. */
        @Editable
        public Tab[] tabs = new Tab[0];

        /** The selected tab. */
        @Editable(min=0)
        public int selected;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.TabbedPane.class) ?
                comp : new com.threerings.opengl.gui.TabbedPane(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.TabbedPane pane = (com.threerings.opengl.gui.TabbedPane)comp;
            Component[] otabs = new Component[pane.getTabCount()];
            for (int ii = 0; ii < otabs.length; ii++) {
                otabs[ii] = pane.getTab(ii);
            }
            pane.removeAllTabs();
            pane.setTabAlignment(tabAlignment.getJustification());
            pane.setGap(gap);
            for (int ii = 0; ii < tabs.length; ii++) {
                Tab tab = tabs[ii];
                Component tcomp = (ii < otabs.length) ? otabs[ii] : null;
                pane.addTab(
                    getMessage(msgs, tab.title),
                    tab.component.getComponent(ctx, scope, msgs, tcomp),
                    tab.hasClose);
            }
            pane.selectTab(selected);
        }
    }

    /**
     * A spacer.
     */
    public static class Spacer extends ComponentConfig
    {
        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Spacer.class) ?
                comp : new com.threerings.opengl.gui.Spacer(ctx);
        }
    }

    /**
     * A slider.
     */
    public static class Slider extends ComponentConfig
    {
        /** The slider's orientation. */
        @Editable
        public Orientation orientation = Orientation.HORIZONTAL;

        /** The slider's model. */
        @Editable
        public BoundedRangeModelConfig model = new BoundedRangeModelConfig();

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return new com.threerings.opengl.gui.Slider(
                ctx, orientation.getConstant(), model.createBoundedRangeModel());
        }
    }

    /**
     * A scroll pane.
     */
    public static class ScrollPane extends ComponentConfig
    {
        /** Whether or not to allow vertical scrolling. */
        @Editable(hgroup="v")
        public boolean vertical = true;

        /** Whether or not to allow horizontal scrolling. */
        @Editable(hgroup="v")
        public boolean horizontal;

        /** The snap value. */
        @Editable(hgroup="s")
        public int snap;

        /** Whether or not to always show the scrollbar. */
        @Editable(hgroup="s")
        public boolean showScrollbarAlways = true;

        /** The style for the viewport, if non-default. */
        @Editable(nullable=true)
        public ConfigReference<StyleConfig> viewportStyle;

        /** The child component. */
        @Editable
        public ComponentConfig child = new Spacer();

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            Component ochild = (comp instanceof com.threerings.opengl.gui.ScrollPane) ?
                ((com.threerings.opengl.gui.ScrollPane)comp).getChild() : null;
            return new com.threerings.opengl.gui.ScrollPane(
                ctx, child.getComponent(ctx, scope, msgs, ochild), vertical, horizontal, snap);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.ScrollPane pane = (com.threerings.opengl.gui.ScrollPane)comp;
            pane.setShowScrollbarAlways(showScrollbarAlways);
            pane.setViewportStyleConfig(viewportStyle);
        }
    }

    /**
     * A scroll bar.
     */
    public static class ScrollBar extends ComponentConfig
    {
        /** The scroll bar's orientation. */
        @Editable
        public Orientation orientation = Orientation.HORIZONTAL;

        /** The scroll bar's model. */
        @Editable
        public BoundedRangeModelConfig model = new BoundedRangeModelConfig();

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return new com.threerings.opengl.gui.ScrollBar(
                ctx, orientation.getConstant(), model.createBoundedRangeModel());
        }
    }

    /**
     * A container.
     */
    public static class Container extends ComponentConfig
    {
        /** The layout of the container. */
        @Editable
        public LayoutConfig layout = new LayoutConfig.Absolute();

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.Container.class) ?
                comp : new com.threerings.opengl.gui.Container(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            layout.configure(ctx, scope, msgs, (com.threerings.opengl.gui.Container)comp);
        }
    }

    /**
     * A config-based user interface component.
     */
    public static class UserInterface extends ComponentConfig
    {
        /** The user interface reference. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.UserInterface.class) ?
                comp : new com.threerings.opengl.gui.UserInterface(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.UserInterface ui =
                (com.threerings.opengl.gui.UserInterface)comp;
            ui.getScope().setParentScope(scope);
            ui.setConfig(userInterface);
            ScopeUtil.call(scope, "registerComponents", ui.getTagged());
        }
    }

    /**
     * An HTML view.
     */
    public static class HTMLView extends ComponentConfig
    {
        /** The view's stylesheet. */
        @Editable
        public String stylesheet = "";

        /** The contents of the view. */
        @Editable
        public String contents = "";

        /** Whether or not the view should be antialiased. */
        @Editable
        public boolean antialias = true;

        @Override // documentation inherited
        protected Component maybeRecreate (
            GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            return (getClass(comp) == com.threerings.opengl.gui.text.HTMLView.class) ?
                comp : new com.threerings.opengl.gui.text.HTMLView(ctx);
        }

        @Override // documentation inherited
        protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
        {
            super.configure(ctx, scope, msgs, comp);
            com.threerings.opengl.gui.text.HTMLView view =
                (com.threerings.opengl.gui.text.HTMLView)comp;
            view.setAntialiased(antialias);
            view.setStyleSheet(stylesheet);
            view.setContents(contents);
        }
    }

    /** The component alpha value. */
    @Editable(min=0, max=1, step=0.01, weight=1, hgroup="c")
    public float alpha = 1f;

    /** Whether or not the component is enabled. */
    @Editable(weight=1, hgroup="c")
    public boolean enabled = true;

    /** Whether or not the component is visible. */
    @Editable(weight=1, hgroup="c")
    public boolean visible = true;

    /** The text for the component's tooltip. */
    @Editable(weight=1, hgroup="t")
    public String tooltipText = "";

    /** Whether or not the tooltip is relative to the mouse cursor. */
    @Editable(weight=1, hgroup="t")
    public boolean tooltipRelativeToMouse;

    /** The component's tag. */
    @Editable(weight=1)
    public String tag = "";

    /** The component's event handlers. */
    @Editable(weight=1)
    public HandlerConfig[] handlers = new HandlerConfig[0];

    /** The component's style, if non-default. */
    @Editable(weight=1, nullable=true)
    public ConfigReference<StyleConfig> style;

    /** The preferred size, if non-default. */
    @Editable(weight=1, nullable=true)
    public DimensionConfig preferredSize;

    /**
     * Creates or updates a component for this configuration.
     *
     * @param scope the component's expression scope.
     * @param comp an existing component to reuse, if possible.
     * @return either a reference to the existing component (if reused) or a new component.
     */
    public Component getComponent (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
    {
        comp = maybeRecreate(ctx, scope, msgs, comp);
        configure(ctx, scope, msgs, comp);
        if (!StringUtil.isBlank(tag)) {
            ScopeUtil.call(scope, "registerComponent", tag, comp);
        }
        return comp;
    }

    /**
     * Recreates the component if the supplied component doesn't match the configuration.
     */
    protected abstract Component maybeRecreate (
        GlContext ctx, Scope scope, MessageBundle msgs, Component comp);

    /**
     * Configures the specified component.
     */
    protected void configure (GlContext ctx, Scope scope, MessageBundle msgs, Component comp)
    {
        comp.setAlpha(alpha);
        comp.setEnabled(enabled);
        comp.setVisible(visible);
        comp.setTooltipText(
            StringUtil.isBlank(tooltipText) ? null : getMessage(msgs, tooltipText));
        comp.setTooltipRelativeToMouse(tooltipRelativeToMouse);
        comp.setStyleConfig(style);
        if (preferredSize != null) {
            comp.setPreferredSize(preferredSize.createDimension());
        }
        comp.removeAllListeners(HandlerConfig.Listener.class);
        for (HandlerConfig handler : handlers) {
            comp.addListener(handler.createListener(scope));
        }
    }

    /**
     * Returns the class of the specified object, or <code>null</code> if the reference is null.
     */
    protected static Class getClass (Object object)
    {
        return (object == null) ? null : object.getClass();
    }

    /**
     * Returns the translation for the supplied text if one exists.
     */
    protected static String getMessage (MessageBundle msgs, String text)
    {
        return msgs.exists(text) ? msgs.get(text) : text;
    }
}
