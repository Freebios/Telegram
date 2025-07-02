⬜ Phase 1: Modular Code Extraction — Non-UI Logic First

⏳Extract Data Models
⏳Create POJOs for UI state (ProfileState, User, ChatInfo, etc.).
⏳Replace ad-hoc multiple flags with cohesive state classes.
⏳Extract Data Access Layer (Repository)
⏳Encapsulate all direct calls to MessagesController, ContactsController, ConnectionsManager, etc.
⏳Provide APIs for fetching user, chat info, blocked status, participants, etc.
⏳Extract Notification/Event Handling
⏳Move all NotificationCenter observing and callbacks into a dedicated controller or ViewModel helper.
✅Setup clear methods to subscribe/unsubscribe observers.
⏳Extract Business Logic to ViewModel
⏳Move user status calculations, online count, blocked check, and state transformation logic here.
⏳Implement simple observer pattern (callbacks, interfaces) to notify UI.
⏳Make sure to keep original code internals unchanged except for moving code out.
⬜ Phase 2: UI Components Decomposition

⏳Identify and extract complex UI parts as custom Views or view groups, e.g.:
⏳AvatarImageView (+ animations)
⏳TopView / overlays
❌ProfileGalleryView (avatar pager)
❌ProfileStoriesView and ProfileGiftsView components
❌Custom RecyclerView cells (HeaderCell, TextDetailCell, UserCell, etc.)
❌UndoView, Floating Buttons, Status Views
❌Move all layout creation logic out of ProfileActivity into these components, with clear public APIs like bind(state) or setData(...).
❌Ensure all UI components expose callback interfaces for user interactions (clicks, long presses).
⬜ Phase 3: Fragment Refactor to Thin UI Layer

Refactor ProfileActivity (Fragment) to:
✅Instantiate and hold a ViewModel instance.
⏳Observe ViewModel for state updates and call extracted UI components accordingly.
❌Pass callbacks from UI components back to ViewModel or Fragment as needed.
⏳Remove direct calls to MessagesController, NotificationCenter, etc. — use ViewModel/Repository instead.
⏳Retain all necessary Android lifecycle methods but only for wiring UI and observers.
⬜ Phase 4: Introduce Programmatic UI / MVVM Scaffold

❌Replace large programmatic UI blocks with modular UI components (from Phase 2).
❌Implement data-binding in Fragment: when ViewModel state changes, update views.
❌Handle animations and transitions via dedicated helpers; keep animation logic out of fragment as much as possible.
⬜ Phase 5: Feature-by-Feature Migration & Validation

For each feature or UI section (e.g., avatar display, media gallery, stories, search, menu actions):

❌Implement the feature in the new modular structure.
❌Run existing functional test scenarios and UI regressions.
❌Compare behavior side-by-side with legacy fragment.
❌Fix discrepancies immediately before moving on.
❌Commit changes with clear, descriptive commit messages (e.g., “Migrate avatar rendering to AvatarView with ViewModel binding”).
⬜ Phase 6: Navigation and Transition Handling

❌Refactor existing activity/fragment transitions to use clean animation helpers.
❌If blending shared elements, move to Fragment shared element transitions where possible.
❌Isolate transition animation code in dedicated classes/methods.
❌Test all entry/exit transitions thoroughly.
⬜ Phase 7: Cleanup & Documentation

❌Remove duplicated code and references to old god-class components.
❌Document new architecture decisions and component boundaries.
❌Document API and expected contracts of ViewModel and UI components.
❌Write or update automated and manual test plans reflecting new structure.
⬜ Phase 8: Rollout & Monitor

Deploy the new version behind feature flags if possible.
Monitor crash reports and user feedback closely.
Roll back or hotfix immediately on critical failures.
Gradually remove legacy code as confidence grows.
Bonus: Practical Tips
Separate animation states from pure UI update logic.
Keep UI code reactive to state only, avoid imperative UI mutation scattered everywhere.
Avoid monolithic methods; aim for small well-named, testable functions.
Use explicit interfaces for communication between Fragment ↔ ViewModel ↔ UI components.
Add lifecycle-aware unsubscription from observers to avoid leaks.