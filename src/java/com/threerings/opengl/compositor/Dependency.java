//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.compositor;

import com.threerings.math.Plane;
import com.threerings.math.Rect;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.config.RenderEffectConfig;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Texture;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a dependency to be resolved in the course of rendering: shadows from a particular
 * light, a reflection off a plane, etc.
 */
public abstract class Dependency
{
    /**
     * The base class of the various planar reflection/refraction dependencies.
     */
    public static abstract class Planar extends Dependency
    {
        /** The eye space plane of reflection or refraction. */
        public Plane plane = new Plane();

        /** The bounds of the affected region in normalized device coordinates. */
        public Rect bounds = new Rect();

        @Override // documentation inherited
        public void merge (Dependency dependency)
        {
            bounds.addLocal(((Planar)dependency).bounds);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return plane.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Planar)other).plane.equals(plane);
        }
    }

    /**
     * A stencil reflection.
     */
    public static class StencilReflection extends Planar
    {
    }

    /**
     * A stencil refraction.
     */
    public static class StencilRefraction extends Planar
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((StencilRefraction)other).ratio == ratio;
        }
    }

    /**
     * The base class for planar reflection/refraction textures.
     */
    public static abstract class PlanarTexture extends Planar
    {
        /** The texture to which we render. */
        public Texture texture;

        @Override // documentation inherited
        public void merge (Dependency dependency)
        {
            texture = ((PlanarTexture)dependency).texture;
        }
    }

    /**
     * A planar reflection texture.
     */
    public static class ReflectionTexture extends PlanarTexture
    {
    }

    /**
     * A planar refraction texture.
     */
    public static class RefractionTexture extends PlanarTexture
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((RefractionTexture)other).ratio == ratio;
        }
    }

    /**
     * A cube map texture.
     */
    public static class CubeTexture extends Dependency
    {
        /** The render origin in eye space. */
        public Vector3f origin = new Vector3f();

        /** The texture to which we render. */
        public Texture texture;

        @Override // documentation inherited
        public void merge (Dependency dependency)
        {
            texture = ((CubeTexture)dependency).texture;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return origin.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((CubeTexture)other).origin.equals(origin);
        }
    }

    /**
     * The base class for shadows from a single light.
     */
    public static abstract class Shadows extends Dependency
    {
        /** The light casting the shadows. */
        public Light light;

        @Override // documentation inherited
        public int hashCode ()
        {
            return light.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Shadows)other).light.equals(light);
        }
    }

    /**
     * Stencil shadow volumes.
     */
    public static class ShadowVolumes extends Shadows
    {
    }

    /**
     * A shadow depth texture.
     */
    public static class ShadowTexture extends Shadows
    {
        /** The shadow texture. */
        public Texture texture;

        @Override // documentation inherited
        public void merge (Dependency dependency)
        {
            texture = ((ShadowTexture)dependency).texture;
        }
    }

    /**
     * A render effect.
     */
    public static class RenderEffect extends Dependency
    {
        /** The configuration of the effect. */
        public RenderEffectConfig config;

        @Override // documentation inherited
        public void resolve (Compositor compositor)
        {
            compositor.addDependencyEffect(config);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return System.identityHashCode(config);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return other instanceof RenderEffect && ((RenderEffect)other).config == config;
        }
    }

    /**
     * Merges another dependency (for which {@link #equals} returns true) into this one.
     */
    public void merge (Dependency dependency)
    {
        // nothing by default
    }

    /**
     * Resolves this dependency.
     */
    public void resolve (Compositor compositor)
    {
    }
}
