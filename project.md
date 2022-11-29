Code
- refactor so that cli.clj uses core.cljc methods
  + change core so it works with cli.clj directly
  - update-with-answer!
	- change the ai :messages updates to use the chat api

- refactor to have web.clj*s* use core.clj also
	- use of cljs-http VS http-kit 
		=> isolate in one call, externalize it?
	- llm-http-request 
		- openai_api_key
			- use lambda function instead of env
		- object to json string
	- parse-llm-response
		- jsons string to object
- add logging to core.cljc  

## Code Backlog

### Refactoring
- move loading logic to cljs-chat
- pure css/html loading

### UX
- faire mieux sur mobile

### Other (chat-related, ops...)
- better solution to load css rather than github links to your repo
- talk about it in clojureverse
- add timestamp
- add status
- Remove bootstrap dependency
- secure the calls to api 

## Product backlog
- Lire (et noter les idées utiles)
  - embeddings
  - guide text completion
  - guide code completion

## Réflexions 
Idées de features
- "always on" feel (most likely via shell input from somewhere else)
- Open files, search mail, search google...
- Auto-learn commands? => later
- Transformer ton texte en commandes shell qui sont exécutées

Idées générales
- Can do code generation with javascript & use it with clojurescript
