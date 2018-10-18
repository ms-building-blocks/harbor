package org.augugrumi.harbor.persistence.data;

import org.augugrumi.harbor.persistence.FieldPath;
import org.augugrumi.harbor.persistence.PersistenceRetriever;
import org.augugrumi.harbor.persistence.Query;
import org.augugrumi.harbor.persistence.Result;
import org.augugrumi.harbor.routes.util.exceptions.NoSuchNetworkComponentException;
import org.augugrumi.harbor.util.ConfigManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.augugrumi.harbor.util.ObjectConverter.json2yaml;

public class VirtualNetworkFunction extends AbsNetworkData {

    private final static Logger LOG = ConfigManager.getConfig().getApplicationLogger(VirtualNetworkFunction.class);
    private String vnfDefinition;

    public interface Fields extends Data.Fields {

        String DEFINITION = "definition";
        String NS_CLAIMS = "nsClaims";
    }

    private void clean() {
        vnfDefinition = null;
    }

    @Override
    boolean saveAndClean() {
        if (isValid()) {
            return false;
        }
        JSONObject myselfJson = new JSONObject();
        Yaml yaml = new Yaml();
        JSONObject convertedFromYaml = new JSONObject((Map) yaml.load(vnfDefinition));
        myselfJson.put(Fields.DEFINITION, convertedFromYaml);
        Query myselfQuery = new Query() {
            @Override
            public String getID() {
                return VirtualNetworkFunction.this.getID();
            }

            @Override
            public String getContent() {
                return myselfJson.toString(); // FIXME escape the \n character!
            }
        };
        Result<Void> queryRes = getDB().save(myselfQuery);
        if (queryRes.isSuccessful()) {
            clean();
        }
        return queryRes.isSuccessful();
    }

    VirtualNetworkFunction(String id, String definition) {
        super(id, PersistenceRetriever.getVnfDb());

        vnfDefinition = definition;
    }

    public VirtualNetworkFunction(String id) {
        super(id, PersistenceRetriever.getVnfDb());
    }

    public String getDefinition() throws NoSuchNetworkComponentException {
        checkValidityOrThrow();
        Result<JSONObject> res = getDB().get(getMyQuery());
        if (res.isSuccessful()) {
            return json2yaml(res.getContent(), Fields.DEFINITION);
        } else {
            return "";
        }
    }

    public boolean setDefinition(String newDefinition) {
        return genericSet(new FieldPath(Fields.DEFINITION), newDefinition);
    }

    public List<NetworkService> getNsClaims() throws NoSuchNetworkComponentException {
        checkValidityOrThrow();
        final List<NetworkService> res = new ArrayList<>();
        final Result<JSONObject> qRes = getDB().get(getMyQuery());
        if (qRes.isSuccessful()) {
            final String stringArray = qRes.getContent().optString(Fields.NS_CLAIMS, "[]");
            JSONArray array = new JSONArray(stringArray);
            List<String> services = (List) array.toList();
            for (final String s : services) {
                res.add(new NetworkService(s));
            }
        }
        return res;
    }

    public boolean addNSClaim(NetworkService ns) throws NoSuchNetworkComponentException {
        checkValidityOrThrow();
        synchronized (this) {
            if (!ns.isValid()) {
                throw new NoSuchNetworkComponentException("No NS with id " + ns.getID() + " found");
            }

            List<NetworkService> res = getNsClaims();
            res.add(ns);
            List<String> newIdList = new ArrayList<>();
            for (final NetworkService n : res) {
                newIdList.add(n.getID());
            }
            return genericSet(new FieldPath(Fields.NS_CLAIMS), newIdList);
        }
    }

    public boolean deleteNSClaim(NetworkService ns) throws NoSuchNetworkComponentException {
        checkValidityOrThrow();
        synchronized (this) {
            if (!ns.isValid()) {
                throw new NoSuchNetworkComponentException("No NS with id " + ns.getID() + " found");
            }

            List<NetworkService> res = getNsClaims();
            res.remove(ns);
            List<String> newIdList = new ArrayList<>();
            for (final NetworkService n : res) {
                newIdList.add(n.getID());
            }
            return genericSet(new FieldPath(Fields.NS_CLAIMS), newIdList);
        }
    }

    public int getNsClaimsSize() {
        return getNsClaims().size();
    }

    @Override
    public String toString() {
        JSONObject converted = new JSONObject(super.toString());
        String convert = json2yaml(converted, Fields.DEFINITION);
        converted.put(Fields.DEFINITION, convert);
        return converted.toString();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        String convert = json2yaml(json, Fields.DEFINITION);
        json.put(Fields.DEFINITION, convert);
        return json;
    }
}
