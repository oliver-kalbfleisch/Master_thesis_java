package ik_hand_model;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

public class HandModel {
	public List<Vector3f> vertices= new ArrayList<Vector3f>();
	public List<Vector3f> normals = new ArrayList<Vector3f>();
	public List<Face> faces= new ArrayList<Face>();
	
	public HandModel() {
		
	}
}
