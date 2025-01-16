package com.threerings.opengl.model.tools.fbx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

import com.google.common.io.Files;
import com.google.common.collect.Lists;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.opengl.model.tools.ModelDef;

import static com.threerings.opengl.Log.log;

public class ModelFbxParser
{
    /**
     * Parse the model as well as extract any textures in the fbx into the specified directory.
     */
    public ModelDef parseModel (InputStream in, File dir)
        throws IOException
    {
        ModelDef model = new ModelDef();
        FBXFile fbx = FBXLoader.loadFBXFile("model", in);
        //FbxDumper.Dump(fbx);

        FBXNode root = fbx.getRootNode();

        List<String> textures = Lists.newArrayList();
        extractTextures(root, dir, textures);

        FBXNode geom = root.getNodeFromPath("Objects/Geometry");
        FBXNode norms = geom.getChildByName("LayerElementNormal");
        FBXNode uvs = geom.getChildByName("LayerElementUV");

        double[] vertices = (double[])geom.getChildByName("Vertices").getProperty(0).getData();
        int[] pvi = (int[])geom.getChildByName("PolygonVertexIndex").getProperty(0).getData();
        double[] normals = (double[])norms.getChildByName("Normals").getProperty(0).getData();
        double[] uvData = uvs != null ? (double[])uvs.getChildByName("UV").getProperty(0).getData() : null;
        int[] uvIndex = uvs != null ? (int[])uvs.getChildByName("UVIndex").getProperty(0).getData() : null;

        ModelDef.TriMeshDef trimesh = new ModelDef.TriMeshDef();
        trimesh.name = root.getName();
        trimesh.texture = textures.isEmpty() ? "REPLACE_ME" : textures.get(0);
        //trimesh.translation = new float[] { 0f, -100f, 0f };
        trimesh.translation = new float[] { 0f, 0f, 0f };
        trimesh.rotation = new float[] { .5f, 0f, 0f, 1f };
        trimesh.scale = new float[] { 1f, 1f, 1f };
        trimesh.offsetTranslation = new float[] { 0f, 0f, 0f };
        trimesh.offsetRotation = new float[] { 0f, 0f, 0f, 1f };
        trimesh.offsetScale = new float[]{ 1f, 1f, 1f };

        // Convert polygons to triangles
        int nidx = 0;
        int uidx = 0;
        for (int idx : pvi) {
            ModelDef.Vertex v = new ModelDef.Vertex();
            // Handle negative indices (they mark end of polygon, need to be made positive)
            if (idx < 0) idx = (-idx - 1);

            // Set vertex position
            int vi = idx * 3;
            v.location = new float[] {
                (float)vertices[vi], (float)vertices[vi + 1], (float)vertices[vi + 2]
            };

            // Set normal
            v.normal = new float[] {
                (float)normals[nidx], (float)normals[nidx + 1], (float)normals[nidx + 2]
            };
            nidx += 3;

            // Set UV coordinates if available
            if (uvData != null) {
                int uvIdx = uvIndex[uidx++];
                if (uvIdx == -1) {
                    v.tcoords = new float[2];
                } else {
                    uvIdx *= 2;
                    v.tcoords = new float[] {
                        (float)uvData[uvIdx], (float)uvData[uvIdx + 1]
                    };
                }
            } else {
                v.tcoords = new float[2]; // Or leave null?
            }

            trimesh.addVertex(v);
        }

        model.addSpatial(trimesh);

        return model;
    }

    /**
     * Extract into `dir` any textures found at <em>or below</em> the specified node.
     */
    public void extractTextures (FBXNode node, File dir, List<String> filenames)
        throws IOException
    {
        // recurse on children
        for (int ii = 0, nn = node.getNumChildren(); ii < nn; ++ii) {
            extractTextures(node.getChild(ii), dir, filenames);
        }

        extractTexture(node, dir, filenames);
    }

    private boolean extractTexture (FBXNode node, File dir, List<String> filenames)
        throws IOException
    {
        // For now, we look for a node called "Video"
        // - that has a sub-node called "Type" with a "Clip" string property.
        // - that has a "Filename" with a string property
        // - and finally a "Content" with the file data
        if (!"Video".equals(node.getName())) return false;
        FBXNode type = node.getChildByName("Type");
        FBXNode filename = node.getChildByName("Filename");
        FBXNode content = node.getChildByName("Content");
        if (type == null || filename == null || content == null ||
                type.getNumProperties() != 1 ||
                filename.getNumProperties() != 1 ||
                content.getNumProperties() != 1) return false;

        Object data = content.getProperty(0).getData();
        if (!(data instanceof byte[])) return false;

        if (!"Clip".equals(type.getProperty(0).getData())) return false;

        // TODO?
        String fullname = String.valueOf(filename.getProperty(0).getData());
        int foreslash = fullname.lastIndexOf('/');
        int backslash = fullname.lastIndexOf('\\');
        String basename = fullname.substring(1 + Math.max(foreslash, backslash));
        Files.write((byte[])data, new File(dir, basename));
        //log.info("Wrote", "file", basename);
        filenames.add(basename);
        return true;
    }
}
