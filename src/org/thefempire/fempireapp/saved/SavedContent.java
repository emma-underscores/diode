package org.thefempire.fempireapp.saved;

/**
 * SavedContent.java
 * @author John Zavidniak
 * A class which holds information related to a saved comment. This is
 * used in association with the SQLite Database to save and retrieve
 * a user's saved content
 */

public class SavedContent
{
 
    private int id;
    private String user;
    private String author;
    private String body;
    private String linkId;
    private String commentId;
    private String femdom;
    
    /**
     * @param user the current logged in user
     * @param author the author of the comment 
     * @param body the html of the comment's body
     * @param linkId equivalent to link_id in the comment's ThingInfo
     * @param commentId equivalent to id in the comment's ThingInfo
     * @param femdom the femdom the comment exists in
     */
    public SavedContent(String user, String author, String body, String linkId, String commentId, String femdom)
    {
        this.user = user;
        this.author = author;
        this.body = body;
        this.linkId = linkId;
        this.commentId = commentId;
        this.femdom = femdom;
    }
    
    /**
     * @param id the id of this object in the SQLite database
     * @param user the current logged in user
     * @param author the author of the comment 
     * @param body the html of the comment's body
     * @param linkId equivalent to link_id in the comment's ThingInfo
     * @param commentId equivalent to id in the comment's ThingInfo
     * @param femdom the femdom the comment exists in
     */
    public SavedContent(int id, String user, String author, String body, String linkId,
            String commentId, String femdom)
    {
        this.id = id;
        this.user = user;
        this.author = author;
        this.body = body;
        this.linkId = linkId;
        this.commentId = commentId;
        this.femdom = femdom;
    }
    
    public int getId()
    {
        return id;
    }
    
    public String getUser()
    {
        return user;
    }
    
    public String getAuthor()
    {
        return author;
    }
    
    public String getBody()
    {
        return body;
    }
    
    public String getLinkId()
    {
        return linkId;
    }
    
    public String getCommentId()
    {
        return commentId;
    }
    
    public String getfemdom()
    {
        return femdom;
    }
    
    public void setId(int id)
    {
        this.id = id;
    }
    
    public void setUser(String user)
    {
        this.user = user;
    }
    
    public void setAuthor(String author)
    {
        this.author = author;
    }
    
    public void setBody(String body)
    {
        this.body = body;
    }
    
    public void setLinkId(String linkId)
    {
        this.linkId = linkId;
    }
    
    public void setCommentId(String commentId)
    {
        this.commentId = commentId;
    }
    
    public void setfemdom(String femdom)
    {
        this.femdom = femdom;
    }
    
}