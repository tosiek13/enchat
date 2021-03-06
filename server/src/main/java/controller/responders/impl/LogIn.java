package controller.responders.impl;

import com.google.inject.Inject;
import controller.responders.IMessageResponder;
import controller.responders.exceptions.IncorrectUserStateException;
import controller.utils.cypher.Decryption;
import controller.utils.state.StateManager;
import message.generators.Conversationalist_Disconnected;
import message.generators.Log_In;
import message.generators.Another_User_Logged;
import message.generators.Server_error;
import message.types.UEMessage;
import message.types.UMessage;
import model.Account;
import model.containers.temporary.LoggedUtil;
import model.containers.temporary.RoomManager;
import model.exceptions.ElementNotFoundException;
import model.exceptions.UserWithNickAlreadyLogged;
import model.user.UserState;
import model.containers.permanent.Authentication;
import model.exceptions.IncorrectNickOrPassword;
import server.sender.MessageSender;
import rsa.exceptions.DecryptingException;

import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Responder, that handles LogIn messages.
 *
 * @author Created by tochur on 16.05.15.
 */
public class LogIn implements IMessageResponder {
    private Decryption decryption;
    private StateManager stateManager;
    private MessageSender messageSender;
    private Log_In log_in;
    private Another_User_Logged another_user_logged;
    private Conversationalist_Disconnected conversationalist_disconnected;
    private Authentication authentication;
    private LoggedUtil loggedUtil;
    private RoomManager roomManager;

    /**
     * Creates Responder, that handles LogIN messages.
     * @param decryption Decryption, util user to decrypt message.
     * @param stateManager StateManager, user used to control users UserStates.
     * @param authentication Authentication, util user to users certification.
     * @param messageSender MessageSender, util used to send prepared message (UEMessages).
     * @param messages Log_IN, util used to easily creation of all types of message with id Log_In.
     * @param another_user_logged Another_User_Logged, util used to easily creation of all types of message with id Another_User_Logged.
     * @param conversationalist_disconnected Conversationalist_Disconnected, util used to easily creation of all types of message with id Conversationalist_Disconnected.
     * @param loggedUtil LoggedUtil, util used to log users.
     * @param roomManager RoomManager, util that enables creation connection between users.
     */
    @Inject
    public LogIn(Decryption decryption, StateManager stateManager,Authentication authentication, MessageSender messageSender,
                 Log_In messages, Another_User_Logged another_user_logged, Conversationalist_Disconnected conversationalist_disconnected,
                 LoggedUtil loggedUtil, RoomManager roomManager){
        this.decryption = decryption;
        this.stateManager = stateManager;
        this.authentication = authentication;
        this.messageSender = messageSender;
        this.log_in = messages;
        this.another_user_logged = another_user_logged;
        this.loggedUtil = loggedUtil;
        this.roomManager = roomManager;
        this.conversationalist_disconnected = conversationalist_disconnected;
    }

    /**
     * Starts the responder as a Thread.
     * @param ueMessage UEMessage, message that will be handled by responder.
     */
    @Override
    public void serveEvent(UEMessage ueMessage) {
        this.ueMessage = ueMessage;
        new Thread(this).start();
    }

    /**
     * Function that calls actions on utils passed
     */
    @Override
    public void run() {
        try{
            stateManager.verify(ueMessage);
            uMessage = decryption.decryptMessage(ueMessage);
            readInfo();
            //May save info.
            account = authentication.authenticate(nick, password);

            stateManager.update(authorID, UserState.LOGGED);
            try{
                loggedUtil.add(authorID, account);
            }catch (UserWithNickAlreadyLogged userWithNickAlreadyLogged) {
                cleanOldAccount(userWithNickAlreadyLogged.getUserID());
            }
            answer = log_in.loggedSuccessfully(authorID);
        } catch(IncorrectUserStateException e){
            //Do nothing just ignore the message
        } catch(DecryptingException e) {
            answer = Server_error.unableToDecrypt(authorID);
        } catch (IncorrectNickOrPassword e) {
            answer = new Log_In().badLoginOrPassword(authorID);
        }

        try{
            messageSender.send(answer);
        } catch (IOException e) {
            System.out.println("Unable to send message to user - answer for Log In request.");
        }

    }

    private void cleanOldAccount(Integer idToDel) {
        try {
            try{
                Collection<Integer> othersInRoom = roomManager.leaveRoom(idToDel);
                messageSender.send(conversationalist_disconnected.message(idToDel));
                if(othersInRoom != null){
                    for(Integer id: othersInRoom){
                        messageSender.send(conversationalist_disconnected.message(id));
                    }
                }
            }catch (ElementNotFoundException e){
                //That's OK
            }
            messageSender.send(another_user_logged.ok(idToDel));
            loggedUtil.remove(idToDel);
        }catch (IOException e){
            //Cannot do more
        }
    }

    private void readInfo(){
        authorID = uMessage.getAuthorID();
        String[] strings = uMessage.getPackages().toArray(new String[0]);
        this.nick = strings[0];
        this.password = strings[1];
    }

    private UEMessage ueMessage;
    private UMessage uMessage;
    private Integer authorID;
    private String nick;
    private String password;
    private UEMessage answer;
    private Account account;
}
