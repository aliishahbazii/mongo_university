package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.internal.operation.UpdateOperation;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Updates.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Document> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions");
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
        usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
        return true;
        //TODO > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.

    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {

        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
        Bson updateFilter = new Document("user_id", userId);
        Bson setUpdate = set("jwt", jwt);
        UpdateOptions options = new UpdateOptions().upsert(true);
        sessionsCollection.updateOne(updateFilter,setUpdate,options);
        return true;
        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        Bson emailFilter = Filters.eq("email", email);
        return usersCollection.find(emailFilter).limit(1).first();
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        Bson userIdFilter = Filters.eq("user_id", userId);
        Document sessionDocument = sessionsCollection.find(userIdFilter).limit(1).iterator().tryNext();

        if (sessionDocument == null) {
            return null;
        }

        Session session = new Session();
        session.setUserId((String) sessionDocument.get("user_id"));
        session.setJwt((String) sessionDocument.get("jwt"));
        // userId
        return session;
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
        Bson userIdFilter = Filters.eq("user_id", userId);
        DeleteResult deleteResult = sessionsCollection.deleteOne(userIdFilter);
        //if not deleted
        if (deleteResult.getDeletedCount() < 1) {
            log.warn("User `{}` could not be found in sessions collection.", userId);
        }
        return deleteResult.wasAcknowledged();
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        this.deleteUserSessions(email);
        //TODO> Ticket: User Management - implement the delete user method
        Bson userEmailFilter = Filters.eq("email", email);
        usersCollection.deleteOne(userEmailFilter);

        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
        return true;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
        String field = "preferences";
        Bson userByEmailFilter = Filters.eq("email",email);

        List<Bson> updateQueryList = new LinkedList<Bson>();
        for (Map.Entry<String, ?> up : userPreferences.entrySet()) {
            updateQueryList.add(unset(field+"."+up.getKey()));
            updateQueryList.add(set(field+"."+up.getKey(),up.getValue()));
        }
        //update
        UpdateResult result = usersCollection.updateOne(userByEmailFilter,updateQueryList);

        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        return result.wasAcknowledged();
    }
}
