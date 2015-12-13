## Copyright 2015 JAXIO http://www.jaxio.com
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##    http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
$output.java($WebFaces, "MultiPageMessagesListener")##

$output.requireStatic("com.google.common.collect.Lists.newArrayList")##
$output.require("java.util.Iterator")##
$output.require("java.util.List")##
$output.require("java.util.Map")##
$output.require("javax.faces.application.FacesMessage")##
$output.require("javax.faces.context.FacesContext")##
$output.require("javax.faces.event.PhaseEvent")##
$output.require("javax.faces.event.PhaseId")##
$output.require("javax.faces.event.PhaseListener")##

/**
 * This code comes from: http://ocpsoft.org/java/persist-and-pass-facesmessages-over-page-redirects/
 * 
 * Enables messages to be rendered on different pages from which they were set.
 * 
 * After each phase where messages may be added, this moves the messages from the page-scoped FacesContext to the session-scoped session map.
 * 
 * Before messages are rendered, this moves the messages from the session-scoped session map back to the page-scoped FacesContext.
 * 
 * Only global messages, not associated with a particular component, are moved. Component messages cannot be rendered on pages other than the one on which they
 * were added.
 * 
 * To enable multi-page messages support, add a <code>lifecycle</code> block to your faces-config.xml file. That block should contain a single
 * <code>phase-listener</code> block containing the fully-qualified classname of this file.
 * 
 * @author Jesse Wilson jesse[AT]odel.on.ca
 * @secondaryAuthor Lincoln Baxter III lincoln[AT]ocpsoft.com
 */
public class $output.currentClass implements PhaseListener {

    private static final long serialVersionUID = 1L;
    private static final String sessionToken = "MULTI_PAGE_MESSAGES_SUPPORT";

    // the list of messages restored during beforePhase
    private ThreadLocal<List<FacesMessage>> restoredMessagesHolder = new ThreadLocal<List<FacesMessage>>();

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    /*
     * Check to see if we are "naturally" in the RENDER_RESPONSE phase. If we have arrived here and the response is already complete, then the page is not going
     * to show up: don't display messages yet.
     */
    // TODO: Blog this (MultiPageMessagesSupport)
    @Override
    public void beforePhase(final PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        this.saveMessages(facesContext);

        if (PhaseId.RENDER_RESPONSE.equals(event.getPhaseId()) && !facesContext.getResponseComplete()) {
            this.restoreMessages(facesContext);
        }
    }

    /*
     * Save messages into the session after every phase.
     */
    @Override
    public void afterPhase(final PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        saveMessages(facesContext);
    }

    @SuppressWarnings("unchecked")
    private int saveMessages(final FacesContext facesContext) {
        List<FacesMessage> messages = newArrayList();
        for (Iterator<FacesMessage> iter = facesContext.getMessages(null); iter.hasNext();) {
            messages.add(iter.next());
            iter.remove();
        }

        if (messages.isEmpty()) {
            return 0;
        }

        // remove previously restored messages during beforePhase
        List<FacesMessage> restoredMessages = restoredMessagesHolder.get();
        if (restoredMessages != null){
            messages.removeAll(restoredMessages);
        }
        restoredMessagesHolder.remove();

        Map<String, Object> sessionMap = facesContext.getExternalContext().getSessionMap();
        List<FacesMessage> existingMessages = (List<FacesMessage>) sessionMap.get(sessionToken);
        if (existingMessages != null) {
            existingMessages.addAll(messages);
        } else {
            sessionMap.put(sessionToken, messages);
        }
        return messages.size();
    }

    @SuppressWarnings("unchecked")
    private int restoreMessages(final FacesContext facesContext) {
        Map<String, Object> sessionMap = facesContext.getExternalContext().getSessionMap();
        List<FacesMessage> messages = (List<FacesMessage>) sessionMap.remove(sessionToken);
        restoredMessagesHolder.set(messages);
        
        if (messages == null) {
            return 0;
        }

        int restoredCount = messages.size();
        for (Object element : messages) {
            facesContext.addMessage(null, (FacesMessage) element);
        }
        return restoredCount;
    }
}
