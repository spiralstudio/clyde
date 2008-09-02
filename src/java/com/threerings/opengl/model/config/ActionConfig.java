//
// $Id$

package com.threerings.opengl.model.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Executor;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.mod.Articulated;
import com.threerings.opengl.util.GlContext;

/**
 * Configurations for actions taken by models.
 */
@EditorTypes({ ActionConfig.CallFunction.class, ActionConfig.SpawnTransient.class })
public abstract class ActionConfig extends DeepObject
    implements Exportable
{
    /**
     * Generic action that calls a scoped function.
     */
    public static class CallFunction extends ActionConfig
    {
        /** The name of the function to call. */
        @Editable
        public String name = "";

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, name, Function.NULL);
            return new Executor() {
                public void execute () {
                    fn.call();
                }
            };
        }
    }

    /**
     * Creates a transient model (such as a particle system) and adds it to the scene at the
     * location of one of the model's nodes.
     */
    public static class SpawnTransient extends ActionConfig
    {
        /** The model to spawn. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The node at whose transform the model should be added. */
        @Editable
        public String node = "";

        @Override // documentation inherited
        public Executor createExecutor (GlContext ctx, Scope scope)
        {
            Articulated.Node node = (Articulated.Node)ScopeUtil.call(scope, "getNode", this.node);
            final Transform3D transform = (node == null) ?
                ScopeUtil.resolve(scope, "worldTransform", new Transform3D()) :
                node.getWorldTransform();
            return new Executor() {
                public void execute () {
                }
            };
        }
    }

    /**
     * Creates an executor for this action.
     */
    public abstract Executor createExecutor (GlContext ctx, Scope scope);
}
