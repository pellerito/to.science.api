/**
 * 
 */
package helper.oai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import models.JsonElementModel;
import models.Node;

/**
 * @author aquast
 *
 */
public class JsonLDMapper {

	private ArrayList<Hashtable<String, String>> complexElement =
			new ArrayList<>();
	private ArrayList<JsonElementModel> jemElement = new ArrayList<>();
	private JsonElementModel jEM = null;
	private Hashtable<String, ArrayList<Integer>> index = new Hashtable<>();

	/**
	 * Constructor that integrates the indexing
	 * 
	 * @param node
	 */
	public JsonLDMapper(JsonNode node) {
		jemElement = mapToJsonElementModel(node, new StringBuffer("root"));
		createIndex();
	}

	/**
	 * @param node
	 * @param pBuffer
	 * @return
	 */
	public ArrayList<JsonElementModel> mapToJsonElementModel(JsonNode node,
			StringBuffer pBuffer) {

		Iterator<String> it = node.fieldNames();
		while (it.hasNext()) {
			int l = pBuffer.length();

			String key = it.next();
			if (node.get(key).isValueNode()) {
				Hashtable<String, String> iE = new Hashtable<>();
				complexElement.add(iE);

				if (jEM != null && jEM.isObject()) {
					Hashtable<String, String> ha = jEM.getComplexElementList();
					ha.put(key, node.get(key).asText());
					jEM.setComplexElement(ha);
				} else {
					iE.put(key, node.get(key).asText());
					jEM = new JsonElementModel(pBuffer.toString());
					jEM.setComplexElement(iE);
					jemElement.add(jEM);
				}
			}

			if (node.get(key).isObject()) {
				pBuffer.append("." + key);
				JsonNode complexNode = node.get(key);

				jEM = new JsonElementModel(pBuffer.toString());
				jEM.setComplexElement(new Hashtable<String, String>());
				Hashtable<String, String> ha = jEM.getComplexElementList();
				mapToJsonElementModel(complexNode, pBuffer);
				jemElement.add(jEM);
			}

			if (node.get(key).isArray()) {
				pBuffer.append("." + key);

				JsonNode complexNode = node.get(key);
				jEM = new JsonElementModel(pBuffer.toString());

				Iterator<JsonNode> nIt = complexNode.elements();
				while (nIt.hasNext()) {
					JsonNode arrayNode = nIt.next();
					if (arrayNode.isObject()) {
						jEM = new JsonElementModel(pBuffer.toString());
						jEM.setComplexElement(new Hashtable<String, String>());
						mapToJsonElementModel(arrayNode, pBuffer);
						jemElement.add(jEM);

					} else {
						Hashtable<String, String> iE = new Hashtable<>();
						iE.put(pBuffer.toString(), arrayNode.asText());
						// complexElement.add(iE);
						jEM.addArrayElement(arrayNode.asText());
					}
				}
				if (jEM.isArray()) {
					jemElement.add(jEM);
				}
			}
			pBuffer.setLength(l);

		}
		return jemElement;
	}

	/**
	 * Create Index of all keys and their occurrences within the ArrayList
	 * jemElement Convenience method to facilitate Access to the
	 */
	public void createIndex() {

		Iterator<JsonElementModel> jemIt = jemElement.iterator();
		ArrayList<Integer> position = new ArrayList<>();
		int i = 0;
		while (jemIt.hasNext()) {
			JsonElementModel jEM1 = jemIt.next();
			if (index.containsKey(jEM1.getPath())) {
				position = index.get(jEM1.getPath());
			} else {
				position = new ArrayList<>();
				System.out.println("da");
			}

			int pos = i;
			position.add(Integer.valueOf(pos));
			index.put(jEM1.getPath(), position);
			i++;
		}
	}

	/**
	 * @param path
	 * @return
	 */
	public ArrayList<JsonElementModel> getElementModel(String path) {

		ArrayList<JsonElementModel> result = new ArrayList<>();
		if (index.containsKey(path)) {
			ArrayList<Integer> fieldIndex = index.get(path);
			for (int i = 0; i < fieldIndex.size(); i++) {
				JsonElementModel sJem = jemElement.get(fieldIndex.get(i));
				result.add(sJem);
			}
			return result;
		}
		return null;
	}

	/**
	 * @param path
	 * @return
	 */
	public ArrayList<Hashtable<String, String>> getElement(String path) {

		ArrayList<Hashtable<String, String>> result = new ArrayList<>();
		if (index.containsKey(path)) {
			ArrayList<Integer> fieldIndex = index.get(path);
			for (int i = 0; i < fieldIndex.size(); i++) {
				JsonElementModel sJem = jemElement.get(fieldIndex.get(i));
				if (sJem.isObject()) {
					Hashtable<String, String> element = sJem.getComplexElementList();
					result.add(element);
				} else {
					Hashtable<String, String> element = new Hashtable<>();
					for (int j = 0; j < sJem.getArrayList().size(); j++) {
						element.put("title", sJem.getArrayList().get(j));
						result.add(element);
					}
				}
			}
			return result;
		}
		return null;
	}

}