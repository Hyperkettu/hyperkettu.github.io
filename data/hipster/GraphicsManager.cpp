#include "GraphicsManager.hpp"
#include <ClanLib/display.h>

namespace hipster {

  void GraphicsManager::draw(const Room& current) {
    images[current.getBackgroundId()].draw(graphicContext, 0, 0);		   
  }


  void GraphicsManager::drawCursor(const position_t& position, int r,
                                   const item_map_t& items, 
                                   const std::set<const HotSpot*>& hotSpots) {
    float x = position.first;
    float y = position.second;

    int id = interfaceManager->itemSelected();

    if(id == InterfaceManager::NO_ITEM ||
       interfaceManager->shouldDrawSaveMenu()){

      CL_Draw::line(graphicContext, x, (y - 2), x, (y - 2 - r),  CL_Colorf::white);
      CL_Draw::line(graphicContext, x, (y + 2), x, (y + 2 + r),  CL_Colorf::white);
      CL_Draw::line(graphicContext, (x + 2), y, (x + 2 + r), y,  CL_Colorf::white);
      CL_Draw::line(graphicContext, (x - 2), y, (x - 2 - r), y,  CL_Colorf::white);
    } 
    else { // draw item image
      item_map_t::const_iterator it = items.find(id);
      if (it == items.end())
        throw HipsterException("Tried to draw an item that does not exist.");
      animation_map_t::iterator jt = animations.find(it->second.getAnimationId());
      if (jt == animations.end())
        throw HipsterException("Tried to draw an animation that does not exist.");
      jt->second.draw(graphicContext, x - ITEM_BUTTON_WIDTH/2, y - ITEM_BUTTON_HEIGHT/2);
    }

    if(interfaceManager->mouseOverHotspot() &&
       !interfaceManager->shouldDrawSaveMenu()) {
      
      int id = interfaceManager->getFocusedHotSpot();

      std::set<const HotSpot*>::iterator iter;
      for(iter = hotSpots.begin(); iter != hotSpots.end(); iter++)
	if((*iter)->getId() == id){
	  // position_t pos = (*iter)->getUpperLeftCorner();
          // position_t pos2 =(*iter)->getBottomRightCorner(); 
	  hotSpotFont.draw_text(graphicContext, 
                                std::max(0, static_cast<int>(position.first - 5*(*iter)->getName().length())),
                                position.second, 
                                (*iter)->getName(), 
                                hotSpotFontColor);
	}
    }
  }

  void GraphicsManager::draw(const Room& current, const Character& ego, 
                             const entity_set_t& entities, 
                             const position_t& position,
                             const item_map_t& items,
                             const std::set<const Character*>& talkingCharacters, const std::set<const HotSpot*>& hotSpots) {
    graphicContext.clear();
    draw(current);
    drawEntities(entities);
    drawRoomOverlay(current);
    drawCharacterLines(talkingCharacters);

    if(interfaceManager->inventoryShown())
      drawInventory(ego.getInventory(), items, position_t(67,70), position_t(957,630), fill, border);
    if (interfaceManager->shouldDrawSaveMenu())
      saveMenu->draw();

    drawNotification();

    drawCursor(position, 10, items, hotSpots);
    window.flip();
  }



  std::pair<int, int> GraphicsManager::getAnimationSize(int id) {
    if (animations.count(id) == 0)
      throw HipsterException("Tried to draw an animation that does not exist.");

    CL_Size size = animations[id].get_size();
    return std::pair<int,int>(size.width, size.height);
  }



  int GraphicsManager::loadAnimation(const std::vector<std::string>& filenames) {
    int nextId = animations.size();
    CL_SpriteDescription desc;
    for (std::vector<std::string>::const_iterator it = filenames.begin();
         it != filenames.end(); ++it)
      desc.add_frame(CL_PNGProvider::load(*it));

    CL_Sprite animation(graphicContext, desc);
    animation.set_delay(STANDARD_DELAY);
    
    animations.insert(std::pair<int, CL_Sprite>(nextId, animation));
    return nextId;
  }



  void GraphicsManager::drawInventory(const std::set<int>& inventory,
                                      const std::tr1::unordered_map<int,Item>& items,
                                      const position_t& ulposition,
                                      const position_t& brposition,
                                      const CL_Colorf& background,
                                      const CL_Colorf& border) {      
    CL_Draw::fill(graphicContext, ulposition.first, ulposition.second, 
                  brposition.first, brposition.second, background);
    CL_Draw::box(graphicContext, ulposition.first, ulposition.second, 
                 brposition.first, brposition.second, border); 

    // draw arrow buttons 
    interfaceManager->getUpButton().draw(graphicContext);
    interfaceManager->getDownButton().draw(graphicContext);
      
    size_t count = 0, d = 0;

    // if no item buttons yet
    if(interfaceManager->getItemButtons().size() == 0){
      for(std::set<int>::const_iterator iter = inventory.begin(); iter != inventory.end(); ++iter){
	ItemButton button(*iter, border);
	
       	CL_Slot slot = button.getSignal().connect(interfaceManager, &InterfaceManager::itemButtonClicked);
	interfaceManager->addItemButton(button, slot);
      }
    }

    int mouseOver = -1;
    
    // draw list according to offset;
    for(size_t j = inventoryOffset; j < interfaceManager->getItemButtons().size(); j++){
	
      float topleftX = ulposition.first + BORDER_GAP + (BUTTON_GAP_X + ITEM_BUTTON_WIDTH)*(count % BUTTONS_PER_ROW);
      float topleftY = ulposition.first + BORDER_GAP + (BUTTON_GAP_Y + ITEM_BUTTON_HEIGHT)*(count/ BUTTONS_PER_ROW);

      interfaceManager->getItemButtons()[j].draw(graphicContext, CL_Rectf(topleftX, topleftY, topleftX + ITEM_BUTTON_WIDTH, topleftY + ITEM_BUTTON_HEIGHT));
      int itemId = interfaceManager->getItemButtons()[j].getItemId();
      int animationId = items.find(itemId)->second.getAnimationId();
      animations[animationId].draw(graphicContext, topleftX, topleftY);
      interfaceManager->getItemButtons()[j].setLocation(topleftX, topleftY, ITEM_BUTTON_WIDTH, ITEM_BUTTON_WIDTH);

      if(interfaceManager->getItemButtons()[j].isInside(interfaceManager->getCursorPosition().first, interfaceManager->getCursorPosition().second))
        mouseOver = itemId;

      // all showable drawn
      if(count == ITEM_BUTTON_COUNT - 1)
        break;
	
      d++;
      count++;
    }
      
    // draw remaing empty buttons if any
    while(count != ITEM_BUTTON_COUNT){
	
      float topleftX = ulposition.first + BORDER_GAP + (BUTTON_GAP_X + ITEM_BUTTON_WIDTH)*(count % BUTTONS_PER_ROW);
      float topleftY = ulposition.first + BORDER_GAP + (BUTTON_GAP_Y + ITEM_BUTTON_HEIGHT)*(count/ BUTTONS_PER_ROW);
      drawButtonBorder(graphicContext, CL_Rectf(topleftX, topleftY,
                                                topleftX + ITEM_BUTTON_WIDTH,
                                                topleftY + ITEM_BUTTON_HEIGHT), 
                       border);
      count++;
    }

    //draw name of the item on mouse over
    if(mouseOver != -1)
      inventoryFont.draw_text(graphicContext, interfaceManager->getCursorPosition().first + 10, interfaceManager->getCursorPosition().second + 5, items.find(mouseOver)->second.getName(), inventoryFontColor);
  }

  inline void GraphicsManager::drawButtonBorder(CL_GraphicContext& gc, const CL_Rectf& location, const CL_Colorf& border) const{
   
    CL_Draw::line(gc, (location.left + 1), location.top, (location.left + 5), location.top, border);
    CL_Draw::line(gc, (location.left + 1), location.bottom, (location.left + 5), location.bottom, border);
    CL_Draw::line(gc, (location.right - 5), location.top, (location.right - 1), location.top, border);
    CL_Draw::line(gc, (location.right - 5), location.bottom, (location.right - 1), location.bottom, border);
    CL_Draw::line(gc, location.left, (location.top + 1), location.left, (location.top + 5), border);
    CL_Draw::line (gc, location.left, (location.bottom - 1), location.left, (location.bottom - 5), border);
    CL_Draw::line(gc, location.right, (location.top + 1), location.right, (location.top + 5), border);
    CL_Draw::line(gc, location.right, (location.bottom - 1), location.right, (location.bottom - 5), border);
  }

  void GraphicsManager::changeInventoryOffset(bool forward){

    if(forward){
      if(interfaceManager->getItemButtons().size() - inventoryOffset > ITEM_BUTTON_COUNT){
        inventoryOffset += BUTTONS_PER_ROW;
      }
    }
    else if(inventoryOffset != 0)
      inventoryOffset -= BUTTONS_PER_ROW;
  }

  void GraphicsManager::drawCharacter(Character& ch){
    int animationId = ch.getCurrentAnimation();
    if (animations.count(animationId) == 0)
      throw HipsterException("Tried to draw an animation that does not exist.");

    CL_Sprite sprite = animations[animationId];
    sprite.draw(graphicContext, ch.getPositionUl().first, ch.getPositionUl().second);
    sprite.update();

    CL_Font font(graphicContext, "Tahoma", 10);	
    std::stringstream ss;
    ss << ch.getPosition().first << " " << ch.getPosition().second;
    font.draw_text(graphicContext, 100, 50, ss.str(), CL_Colorf::white);
  }



  void GraphicsManager::drawEntities(const entity_set_t& entities) {
    // iterate over all entities
    for(entity_set_t::const_iterator it = entities.begin();
        it != entities.end(); ++it) {
      // get animation and verify that it exists
      int animationId = (*it)->getCurrentAnimation();
      if (animations.count(animationId) == 0)
        throw HipsterException("Tried to draw an animation that does not exist.");

      // then get the corresponding sprite, draw and update
      CL_Sprite sprite = animations[animationId];
      sprite.draw(graphicContext, (*it)->getPositionUl().first, (*it)->getPositionUl().second);
      sprite.update();
    }   
  }



  int GraphicsManager::loadImage(const std::string& filename) {
    CL_PixelBuffer buf(CL_PNGProvider::load(filename));
    // CL_Image texture(gc, buf, CL_Rect(0,0, width, height));
    CL_Image texture(graphicContext, buf, CL_Rect(0,0, buf.get_width(), buf.get_height()));
    int newId = images.size();
    images[newId] = texture;
    return newId;
  }



  int GraphicsManager::loadMask(const std::string& filename) {
    CL_PixelBuffer buf(CL_PNGProvider::load(filename));
    int newId = masks.size();
    masks[newId] = buf;
    return newId;
  }

  std::vector<bool> generateMask(const CL_String& filename){
    
    CL_PixelBuffer buf(CL_PNGProvider::load(filename));
    std::vector<bool> mask;

    for(int y = 0; y < buf.get_height(); y++)
      for(int x = 0; x < buf.get_width(); x++)
	mask.push_back(buf.get_pixel(x, y) == CL_Colorf::black);
    return mask;
  }



  void GraphicsManager::registerApi(TaskManager& tm) {
    tm.registerFunction(this, &GraphicsManager::loadImage, "loadImage");
    tm.registerFunction(this, &GraphicsManager::loadMask, "loadMask");
    tm.registerFunction(this, &GraphicsManager::setWindowTitle, "setWindowTitle");
    tm.registerFunction(this, &GraphicsManager::showImage, "showImage");
    tm.registerFunction(this, &GraphicsManager::loadAnimation, "loadAnimation");
  }



  void GraphicsManager::setWindowTitle(const std::string& newTitle) {
    window.set_title(newTitle);
  }



  void GraphicsManager::showImage(int id) {
    graphicContext.clear();
    images[id].draw(graphicContext, 0, 0);
    window.flip();
  }



  CL_PixelBuffer& GraphicsManager::getMask(int id) {
    std::tr1::unordered_map<int,CL_PixelBuffer>::iterator it = masks.find(id);
    if (it == masks.end())
      throw HipsterException("No such mask!");
    return it->second;
  }



  void GraphicsManager::drawCharacterLines(const std::set<const Character*>& talkingCharacters) {
    for (std::set<const Character*>::const_iterator it = talkingCharacters.begin();
         it != talkingCharacters.end(); ++it) {
      CL_Font font(graphicContext, (*it)->getFontName(), Character::DIALOGUE_FONT_HEIGHT);
      int y = std::max((*it)->getPositionUl().second, 20);
      int x = (*it)->getPosition().first - (*it)->getLine().length() * 3;
      font.draw_text(graphicContext, x, y, (*it)->getLine(), CL_Colorf::white);
    }
  }



  void GraphicsManager::drawRoomOverlay(const Room& room) {
    int32_t id = room.getOverlayId();
    if (!images.count(id))
      throw HipsterException("Tried to draw an overlay image that does not exist!");
    images[id].draw(graphicContext, 0, 0);		   
  }



  void GraphicsManager::setInterfaceManager(InterfaceManager* im) {
    this->interfaceManager = im;
    this->interfaceManager->setSaveMenu(saveMenu.get());
  }



  GraphicsManager::GraphicsManager(CL_DisplayWindow& window) : 
      window(window), width(window.get_gc().get_width()), 
      height(window.get_gc().get_height()), 
      graphicContext(window.get_gc()), 
      inventoryOffset(0), fill(CL_Colorf(0.0f, 0.4f, 0.8f, 0.2f)), 
      border(CL_Colorf(0.0f, 0.4f, 0.8f, 0.4f)),
      inventoryFontColor(CL_Colorf(0.0f, 0.4f, 0.8f, 0.6f)),
      hotSpotFontColor(CL_Colorf(0.0f, 0.0f, 0.2f, 0.5f)),
      inventoryFont(graphicContext, "Verdana", 32), 
      hotSpotFont(graphicContext, "Verdana", 32),
      saveMenu(new SaveMenu(graphicContext)),
      notificationFont(graphicContext, "Verdana", 32)
    {     
      window.hide_cursor();
    }



  void GraphicsManager::drawNotification() {
    const std::string& notification = interfaceManager->getNotification();
    if (notification.length() > 0)
      notificationFont.draw_text(graphicContext, 
                                 width/2 - 5*notification.length(), 
                                 height/2, notification, CL_Colorf::white);
  }
}
