package MTA.Kaplat.itaybe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.lang.System.currentTimeMillis;


@RestController
public class TODOHandler {
    private List<TODO> TODOList;
    private static int requestCounter = 1;
    private static Logger requestLogger = LogManager.getLogger("request-logger");
    private static Logger TODOLogger = LogManager.getLogger("todo-logger");

    private static String appendRequestNumber = " | request #";

    TODOHandler()
    {
        this.TODOList = new ArrayList<>();
    }

    @GetMapping("/todo/health")
    public String health() {
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo/health | HTTP Verb GET" + appendRequestNumber + requestCounter);
        requestLogger.debug("request #" + requestCounter + " duration: 0ms" + appendRequestNumber + requestCounter);
        requestCounter++;
        return "OK";
    }

    @PostMapping(value="/todo", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> createTODO(@RequestBody String newTODO) throws JsonProcessingException {
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo | HTTP Verb POST" + appendRequestNumber + requestCounter);
        long duration = System.currentTimeMillis();
        String errorMsg;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(newTODO);

        String title = node.get("title").textValue();
        for(TODO todo : this.TODOList)
        {
            if(Objects.equals(todo.title, title)) {
                duration = System.currentTimeMillis() - duration;
                requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
                errorMsg = "Error: TODO with the title [" + title + "] already exists in the system";
                TODOLogger.error(errorMsg + appendRequestNumber + requestCounter);
                requestCounter++;
                return ResponseEntity.status(409).body(errorMessage(errorMsg));
            }
        }

        long dueDate = node.get("dueDate").longValue();
        if (dueDate < currentTimeMillis()) {
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            errorMsg = "Error: Can't create new TODO that its due date is in the past";
            TODOLogger.error(errorMsg + appendRequestNumber + requestCounter);
            requestCounter++;
            return ResponseEntity.status(409).body(errorMessage(errorMsg));
        }

        String content = node.get("content").textValue();
        int currID = TODO.getIDGen();
        TODOLogger.info("Creating new TODO with Title [" + title + "]" + appendRequestNumber + requestCounter);
        TODOLogger.debug("Currently there are " + (TODO.getIDGen() - 1) + " TODOs in the system. New TODO will be assigned with id " + TODO.getIDGen() + appendRequestNumber + requestCounter);
        TODO newTODOObject = new TODO(title, content, dueDate);
        this.TODOList.add(newTODOObject);

        duration = System.currentTimeMillis() - duration;
        requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
        requestCounter++;
        return ResponseEntity.status(200).body(result(Integer.toString(currID)));
    }

    @GetMapping ("/todo/size")
    public ResponseEntity<String> getTODOsCount(@RequestParam String status)
    {
        int counter = 0;
       switch(status){
           case "ALL":
               requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo/size | HTTP Verb GET" + appendRequestNumber + requestCounter);
               requestLogger.debug("request #" + requestCounter + " duration: 0ms" + appendRequestNumber + requestCounter);
               TODOLogger.info("total TODOs count for state " + status + " is " + this.TODOList.size() + appendRequestNumber + requestCounter);
               requestCounter++;
               return ResponseEntity.status(200).body(result(Integer.toString(this.TODOList.size())));
           case "PENDING":
           case "LATE":
           case "DONE":
               requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo/size | HTTP Verb GET" + appendRequestNumber + requestCounter);
               long duration = System.currentTimeMillis();
               for(TODO todo : this.TODOList)
               {
                   if(Objects.equals(todo.status, status))
                       counter++;
               }
               duration = System.currentTimeMillis() - duration;
               requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
               TODOLogger.info("total TODOs count for state " + status + " is " + counter + appendRequestNumber + requestCounter);
               requestCounter++;
               return ResponseEntity.status(200).body(result(Integer.toString(counter)));
           default:
               return ResponseEntity.status(400).body("");
       }
    }

    @GetMapping ("/todo/content")
    public ResponseEntity<String> getTODOsData(@RequestParam(name = "status") String status, @RequestParam(name = "sortBy", required = false) String sortBy)
    {
        List<TODO> todos;
        long duration = System.currentTimeMillis();
        switch(status)
        {
            case "ALL":
                todos = this.TODOList;
                break;
            case "PENDING":
            case "LATE":
            case "DONE":
                todos = getTODOsByStatus(status);
                break;
            default:
                return ResponseEntity.status(400).body("");
        }

        if(sortBy == null || sortBy.equals("ID"))
            TODO.compareBy = TODO.compareByOptions.ID.ordinal();
        else{
            switch(sortBy) {
                case "DUE_DATE":
                    TODO.compareBy = TODO.compareByOptions.DUE_DATE.ordinal();
                    break;
                case "TITLE":
                    TODO.compareBy = TODO.compareByOptions.TITLE.ordinal();
                    break;
                default:
                    return ResponseEntity.status(400).body("");
            }
        }
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo/content | HTTP Verb GET" + appendRequestNumber + requestCounter);
        TODOLogger.info("Extracting todos content. Filter: " + status + " | Sorting by: " + sortBy + appendRequestNumber + requestCounter);
        TODOLogger.debug("There are a total of " + this.TODOList.size() + " todos in the system. The result holds " + todos.size() + " todos" + appendRequestNumber + requestCounter);
        Collections.sort(todos);
        duration = System.currentTimeMillis() - duration;
        requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
        requestCounter++;
        return ResponseEntity.status(200).body(result(TODOHandler.TODOListToJsonArray(todos)));
    }

    private List<TODO> getTODOsByStatus(String status)
    {
        List<TODO> todos = new ArrayList<>();
        for(TODO todo : this.TODOList)
        {
            if(Objects.equals(todo.status, status))
                todos.add(todo);
        }
        return todos;
    }

    private static String TODOListToJsonArray(List<TODO> todos)
    {
        StringBuilder jsonArray = new StringBuilder("[\n");
        final String endl = ",\n";
        for(int i = 0; i < todos.size(); i++)
        {
            jsonArray.append("{\n");
            jsonArray.append("\"id\": ").append(todos.get(i).ID).append(endl);
            jsonArray.append("\"title\": \"").append(todos.get(i).title).append("\"").append(endl);
            jsonArray.append("\"content\": \"").append(todos.get(i).content).append("\"").append(endl);
            jsonArray.append("\"status\": \"").append(todos.get(i).status).append("\"").append(endl);
            jsonArray.append("\"dueDate\": ").append(todos.get(i).dueDate).append(endl);
            if(i == todos.size() - 1)
                jsonArray.append("}\n");
            else
                jsonArray.append("},\n");
        }
        jsonArray.append("]");

        return jsonArray.toString();
    }

    @PutMapping ("/todo")
    public ResponseEntity<String> updateTODOStatus(@RequestParam(name="id") int id, @RequestParam(name="status") String status)
    {
        long duration = System.currentTimeMillis();
        for(TODO todo : this.TODOList)
        {
            if(todo.ID == id)
            {
                if(Objects.equals(status, "PENDING") || Objects.equals(status, "LATE") || Objects.equals(status, "DONE")) {
                    requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo | HTTP Verb PUT" + appendRequestNumber + requestCounter);
                    TODOLogger.info("Update TODO id [" + id + "] state to " + status + appendRequestNumber + requestCounter);
                    String oldStatus = todo.updateTODO(status);
                    TODOLogger.debug("Todo id [" + id + "] state change: " + oldStatus + " --> " + status + appendRequestNumber + requestCounter);
                    duration = System.currentTimeMillis() - duration;
                    requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
                    requestCounter++;
                    return ResponseEntity.status(200).body(result("\"" + oldStatus + "\""));
                }
                else
                    return ResponseEntity.status(400).body("");
            }
        }
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo | HTTP Verb PUT" + appendRequestNumber + requestCounter);
        TODOLogger.info("Update TODO id [" + id + "] state to " + status + appendRequestNumber + requestCounter);
        duration = System.currentTimeMillis() - duration;
        requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
        String errorMsg = "Error: no such TODO with id " + id;
        TODOLogger.error(errorMsg + appendRequestNumber + requestCounter);
        requestCounter++;
        return ResponseEntity.status(404).body(errorMessage(errorMsg));
    }

    @DeleteMapping ("/todo")
    public ResponseEntity<String> deleteTODO(@RequestParam int id)
    {
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /todo | HTTP Verb DELETE" + appendRequestNumber + requestCounter);
        long duration = System.currentTimeMillis();
        for(TODO todo : this.TODOList)
        {
            if(todo.ID == id)
            {
                TODOLogger.info("Removing todo id " + id + appendRequestNumber +requestCounter);
                this.TODOList.remove(todo);
                TODOLogger.debug("After removing todo id [" + id + "] there are " + this.TODOList.size() + " TODOs in the system" + appendRequestNumber + requestCounter);
                duration = System.currentTimeMillis() - duration;
                requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
                requestCounter++;
                return ResponseEntity.status(200).body(result(Integer.toString(this.TODOList.size())));
            }
        }
        duration = System.currentTimeMillis() - duration;
        requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
        requestCounter++;
        String errorMsg = "Error: no such TODO with id " + id;
        TODOLogger.error(errorMsg + appendRequestNumber + requestCounter);
        return ResponseEntity.status(404).body(errorMessage(errorMsg));
    }

    public String result(String res)
    {
        return "{\n\"result\": " + res + "\n}";
    }

    public String errorMessage(String msg)
    {
        return "{\n\"errorMessage\": \"" + msg + "\"\n}";
    }

    @GetMapping("/logs/level")
    public String getLogLevel(@RequestParam(name="logger-name") String loggerName)
    {
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /logs/level | HTTP Verb GET" + appendRequestNumber + requestCounter);
        long duration = System.currentTimeMillis();
        if(Objects.equals(loggerName, requestLogger.getName()))
        {
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            requestCounter++;
            return "Success: " + requestLogger.getLevel().toString();
        }
        else if(Objects.equals(loggerName, TODOLogger.getName()))
        {
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            requestCounter++;
            return "Success: " + TODOLogger.getLevel().toString();
        }
        else {
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            requestCounter++;
            return "Failure: no logger with the name \"" + loggerName + "\" exists.";
        }
    }

    @PutMapping("/logs/level")
    public String setLogLevel(@RequestParam(name="logger-name") String loggerName, @RequestParam(name="logger-level") String loggerLevel)
    {
        requestLogger.info("Incoming request | #" + requestCounter + " | resource: /logs/level | HTTP Verb PUT" + appendRequestNumber + requestCounter);
        long duration = System.currentTimeMillis();
        if(Objects.equals(loggerName, requestLogger.getName()) || Objects.equals(loggerName, TODOLogger.getName()))
        {
            String toReturn = setLogLevelForLogger(loggerName, loggerLevel);
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            requestCounter++;
            return toReturn;
        }
        else
        {
            duration = System.currentTimeMillis() - duration;
            requestLogger.debug("request #" + requestCounter + " duration: " + duration + "ms" + appendRequestNumber + requestCounter);
            requestCounter++;
            return "Failure: no logger with the name \"" + loggerName + "\" exists.";
        }
    }

    private static String setLogLevelForLogger(String loggerName, String loggerLevel)
    {
        switch(loggerLevel)
        {
            case "ERROR":
                Configurator.setLevel(loggerName, Level.ERROR);
                return "Success: " + loggerLevel;
            case "INFO":
                Configurator.setLevel(loggerName, Level.INFO);
                return "Success: " + loggerLevel;
            case "DEBUG":
                Configurator.setLevel(loggerName, Level.DEBUG);
                return "Success: " + loggerLevel;
            default:
                return "Failure: not a valid log level.";
        }
    }
}
