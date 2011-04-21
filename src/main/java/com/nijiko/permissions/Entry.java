package com.nijiko.permissions;

//import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.nijiko.data.IStorage;

public abstract class Entry {

    protected ModularControl controller;
    protected IStorage data;
    protected String name;
    protected String world;
    protected static EntryType type;

   Entry(ModularControl controller, IStorage data, String name, String world) {
        this.controller = controller;
        this.data = data;
        this.name = name;
        this.world = world;
    }
   
    public boolean canBuild() {
        return data.canBuild(world, name, type);
    }
    public String getPrefix() {
        return data.getPrefix(world, name, type);
    }
    public String getSuffix() {
        return data.getSuffix(world, name, type);
    }
    public Set<String> getPermissions() {
        return data.getPermissions(world, name, type);
    }
    public Set<String> getParents() {
        return data.getParents(world, name, type);
    }

    public void setBuild(final boolean build) {
        data.setBuild(world, name, type, build);
    }
    public void setPrefix(final String prefix) {
        data.setPrefix(world, name, type,prefix);
    }
    public void setSuffix(final String suffix) {
        data.setSuffix(world, name, type, suffix);
    }

    
    public void setPermission(final String permission, final boolean add) {
        Set<String> permissions = this.getPermissions();
        String negated = permission.startsWith("-") ? permission.substring(1) : "-" + permission;
        if(add)
        {
            if(permissions.contains(negated))
            {
                data.removePermission(world, name, type, negated);
            }
            data.addPermission(world, name, type, permission);
        }
        else
        {
            data.removePermission(world, name, type, permission);
            data.addPermission(world, name, type, negated);
        }
    }

    public void addPermission(final String permission)
    {
        this.setPermission(permission, true);
    }

    public void removePermission(final String permission)
    {
        this.setPermission(permission, false);
    }

    public void addParent(Group group)
    {
        data.addParent(world, name, group.world, group.name);
    }
    
    public void removeParent(Group group)
    {
        if(this.inGroup(group.world, group.name)) data.removeParent(world, name, group.world, group.name);        
    }
    public boolean hasPermission(String permission)
    {
        Set<String> permissions = this.getPermissions();
        Set<String> groupPermissions = this.getInheritancePermissions();
        if( (permissions == null || permissions.isEmpty()) && (groupPermissions == null || groupPermissions.isEmpty()) ) return false;

        //Do it in +user -> -user -> +group -> -group order
        if(permissions.contains(permission)) return true;
        if(permissions.contains("-" + permission)) return false;
        if(groupPermissions.contains(permission)) return true;
        if(groupPermissions.contains("-" + permission)) return true;



        String[] nodeHierachy = permission.split("\\.");
        //  nodeHierachy = Arrays.copyOfRange(nodeHierachy, 0, nodeHierachy.length);
        String nextNode = "";
        String wild = "";
        String negated = "";
        String relevantNode = permissions.contains("-*") ? (permissions.contains("*") ? "*" : "-*") : "";
        for(String nextLevel : nodeHierachy)
        {
            nextNode += nextLevel + ".";
            wild = nextNode + "*";
            negated = "-" + wild;
            if (permissions.contains(wild)) {
                relevantNode = wild;
                continue;
            }

            if (permissions.contains(negated)) {
                relevantNode = negated;
                continue;
            }

            if (groupPermissions.contains(wild)) {
                relevantNode = wild;
                continue;
            }

            if (groupPermissions.contains(negated)) {
                relevantNode = negated;
                continue;
            } 
        }

        return !relevantNode.startsWith("-");        
    }

    public Set<String> getInheritancePermissions()
    {
        Set<String> permSet = new HashSet<String>();
        Set<Group> groupSet = this.getAncestors();
        for(Group grp: groupSet)
        {
            permSet.addAll(grp.getPermissions());
        }
        return permSet;
    }
    
    protected Set<Group> getAncestors() {
        Set<Group> groupSet = new HashSet<Group>();
        Queue<Group> queue = new LinkedList<Group>();

        //Start with the direct ancestors
        Set<Group> parents = controller.stringToGroups(this.world, this.getParents());
        if(parents!=null && parents.size() > 0) queue.addAll(parents);

        //Poll the queue
        while(queue.peek() != null) {
            Group grp = queue.poll();
            if(grp == null || groupSet.contains(grp)) continue;
            parents = controller.stringToGroups(grp.world, grp.getParents());
            if(parents!=null && parents.size() > 0) queue.addAll(parents);
            groupSet.add(grp);
        }

        return groupSet;
    }

    protected boolean inGroup(String world, String group, Set<Group> checked)
    {
        Set<Group> parents = controller.stringToGroups(this.world, getParents());
        if(parents == null) return false;
        for(Group grp : parents)
        {
            if(checked.contains(grp)) continue;
            checked.add(grp);
            if(grp.world.equalsIgnoreCase(world) && grp.name.equalsIgnoreCase(group)) return true;
            if(grp.inGroup(world, group, checked)) return true;
        }
        return false;
    }
    
    public boolean inGroup(String world, String group)
    {
        if(this.getType()==EntryType.GROUP && this.world.equalsIgnoreCase(world) && this.name.equalsIgnoreCase(group)) return true;
        Set<Group> checked = new HashSet<Group>();
        return this.inGroup(world, group, checked);
    }
    
    public Set<String> getGroups()
    {
        Set<Group> groupSet = this.getAncestors();
        Set<String> nameSet = new HashSet<String>();
        for(Group grp : groupSet)
        {
            if(grp!=null)nameSet.add(grp.name);
        }
        return nameSet;
    }
    
    public abstract EntryType getType();
}

