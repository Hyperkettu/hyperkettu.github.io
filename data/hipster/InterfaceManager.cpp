#include "InterfaceManager.hpp"

namespace hipster {
  void InterfaceManager::handleInput(Character& ch, Room& room, 
                                     const std::set<const HotSpot*>& hotspots) {
    // check for hot spots
    const HotSpot* hotSpot = HotSpot::hotSpotAt(hotspots, getCursorPosition());
    if(hotSpot != NULL) {
      mouseOverHotSpot = true;
      focusedHotSpot = hotSpot->getId();
    }
    else
      mouseOverHotSpot = false;


    switch(mouseInput) {
  
    case CL_MOUSE_LEFT:
    case CL_MOUSE_RIGHT:
      // if notification is being displayed, reset it. Otherwise,
      // check if an item is selected, and if it is, use on target
      // otherwise, activate target (either look at or use)
      // if the curser is not hovering above a hot spot, just walk to the target
      if (notification.length() > 0)
        notification = "";
      else if(!showInventory && !isSaveMenuOpen) {
        if (mouseOverHotSpot)
          if (selectedItem == NO_ITEM) {
            if (mouseInput == CL_MOUSE_RIGHT)
              gameManager.useHotSpot(focusedHotSpot);
            else
              gameManager.lookAtHotSpot(focusedHotSpot);
          }
          else
            gameManager.useItemOnHotSpot(selectedItem, focusedHotSpot);
        else
          ch.walkTo(position_t(mouse.get_x(), mouse.get_y()), graphicsManager->getMask(room.getTraversabilityMaskId()));
        selectedItem = NO_ITEM;
      }
      else if (showInventory) {
	for(std::vector<ItemButton>::iterator iter = itemButtons.begin(); iter != itemButtons.end(); iter++){
	  if(iter->isInside(mouse.get_x(), mouse.get_y())){
	    iter->click();
	    break;
	  }
	}

	if(upArrow.isInside(mouse.get_x(), mouse.get_y()))
	  upArrow.click();
	
	if(downArrow.isInside(mouse.get_x(), mouse.get_y()))
	  downArrow.click();
      }
      else if (isSaveMenuOpen) {
        // Saving or loading may fail. If that happens, create a notification.
        try {
          saveMenu->click(position_t(mouse.get_x(), mouse.get_y()), gameManager);
        }
        catch (HipsterException& e) {
          notification = e.what();
        }
      }
      mouseInput = MOUSE_HANDLED;
      break;

    default:
      break;
    }
  }



  void InterfaceManager::mousePressed(const CL_InputEvent& event, const CL_InputState& state){
    switch(event.id) {
    
    case CL_MOUSE_LEFT:
      mouseInput = CL_MOUSE_LEFT;
      break;

    case CL_MOUSE_RIGHT:
      mouseInput = CL_MOUSE_RIGHT;
      break;

    default:
      break;
    }
  }



  void InterfaceManager::keyPressed(const CL_InputEvent& event, const CL_InputState& state){ 
    switch(event.id){

    case CL_KEY_TAB:
      tabPressed();
      break;

    case CL_KEY_F5:
      isSaveMenuOpen = !isSaveMenuOpen;
      saveMenu->updateLineContent(gameManager);
      break;
      
    default:
      break;
      
    }
  }



  void InterfaceManager::mouseReleased(const CL_InputEvent& event, const CL_InputState& state) {
    
    if(showInventory){
      upArrow.release();
      downArrow.release();
    }
  }




  void InterfaceManager::tabPressed(){
  
    showInventory = !showInventory;  
    
    if(!showInventory){
 
      itemButtons.clear();
      itemSlots.clear();
      graphicsManager->resetInventoryOffset();

    } else {

      upArrow = ArrowButton(graphicsManager->getFillColor(), graphicsManager->getBorderColor(), true);
      CL_Slot slot = upArrow.getSignal().connect(graphicsManager, &GraphicsManager::changeInventoryOffset);
      upArrow.setLocation(3,300, 50, 40);
      itemSlots.push_back(slot);

      downArrow = ArrowButton(graphicsManager->getFillColor(), graphicsManager->getBorderColor(), false);
      CL_Slot slot2 = downArrow.getSignal().connect(graphicsManager, &GraphicsManager::changeInventoryOffset);
      downArrow.setLocation(3,345, 50, 40);
      itemSlots.push_back(slot2);
    }
  }



  void InterfaceManager::addItemButton(const ItemButton& button,
                                       const CL_Slot& slot) {
    itemSlots.push_back(slot);
    itemButtons.push_back(button);
  }



  void InterfaceManager::registerApi(TaskManager& tm) {
    tm.registerFunction(this, &InterfaceManager::setNotification, "notify");
  }
}
