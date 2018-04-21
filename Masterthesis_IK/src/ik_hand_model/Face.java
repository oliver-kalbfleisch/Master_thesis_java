package ik_hand_model;

import org.joml.Vector3f;

public class Face {
	private Vector3f vertex= new Vector3f();// three indces
	private Vector3f normal= new Vector3f();
	
	public Face(Vector3f vertex,Vector3f normal)
	{
		this.setNormal(normal);
		this.setVertex(vertex);
	}

	public Vector3f getVertex() {
		return vertex;
	}

	public void setVertex(Vector3f vertex) {
		this.vertex = vertex;
	}

	public Vector3f getNormal() {
		return normal;
	}

	public void setNormal(Vector3f normal) {
		this.normal = normal;
	}
}

