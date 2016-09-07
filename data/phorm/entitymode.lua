------------------------------------------------------------
-- EntityMode
------------------------------------------------------------

local EditorMode = require "game.leveleditor.editormode"
local EntityFactory = require "game.entityfactory"
local PipeLine = require "game.entities.pipeline"
local PipeCreatorGUI = require "game.entities.pipecreatorgui"

-- Entity mode

local EntityMode = class(EditorMode, "EntityMode")

function EntityMode:init(levelEditor)

  EditorMode.init(self, levelEditor)
  
  self.levelEditor = levelEditor

  self:createGUI()

  self.selectedEntity = nil

  self.moveEntity = false
  local game = getCurState()
  self.level = game:getLevel()

  self.offsetX = 0
  self.offsetY = 0

end

function EntityMode:createEntity(className)

  local x,y = getCurState():getCamera():getLoc()
  local name = "entity" .. self.level:getEntityCount()
  local entity = EntityFactory.createEntity(className)

  entity:setPosition(x,y)
  entity:setName(name)
  
  self:checkForSpecialEntities(entity)

  self.level:addEntity(name, entity)

  self.selectedEntity = entity

  self:showImageGUI()

end

function EntityMode:destroyEntity()

end

function EntityMode:createGUI()

  local gui = getCurState():getGUI()

  -- Load piece mode layout
  local roots, widgets, groups = gui:loadLayout(self.levelEditor:getResources().getPath("entitymodelayout.lua"), nil, self.levelEditor:getWidgets().bg.window)
  self.widgets = widgets

  self.nameEdit = self.widgets.nameEdit
  self.nameLabel = self.widgets.nameLabel
  self.infoLabel = self.widgets.infoLabel.window

  local w = self.widgets.nameEdit.window
    w:registerEventHandler(w.EVENT_EDIT_BOX_TEXT_ACCEPTED, nil,
        function()
        if self.selectedEntity ~= nil then
          local newName = self.widgets.nameEdit.window:getText()

          self.selectedEntity = self.level:removeEntity(self.selectedEntity:getName())
          self.selectedEntity:setName(newName)
          self.level:addEntity(newName, self.selectedEntity)
        end
      end)

  local entityList = self.widgets.entityList.window
  self.entityList = entityList
  for name, class in pairs(EntityFactory.getClassList()) do
    local row = entityList:addRow()
    row:getCell(1):setText(name)
  end

  local select = self.widgets.selectButton.window
  select:registerEventHandler(select.EVENT_BUTTON_CLICK, nil,
    function()
      local selections = entityList:getSelections()
      if #selections >= 1 then
        local cell = entityList:getRow(selections[1]):getCell(1)
        self:createEntity(cell:getText())
      end
    end)

  self:hideGUI()

end

function EntityMode:showGUI()

  for k, v in pairs(self.widgets) do
    v.window:show()
  end

  if self.selectedEntity == nil then
    self:hideImageGUI()
  end

end

function EntityMode:hideImageGUI()

  if self.nameEdit ~= nil and self.nameLabel ~= nil then

    self.nameEdit.window:hide()
    self.nameLabel.window:hide()
  end

end

function EntityMode:showImageGUI()

  if self.nameEdit ~= nil and self.nameLabel ~= nil then

    self.nameEdit.window:show()
    self.nameLabel.window:show()

    if self.selectedEntity ~= nil then

      if self.selectedEntity:getName() ~= nil then
        self.nameEdit.window:setText(self.selectedEntity:getName())
      else
        self.nameEdit.window:setText("")
      end

    end

  end

end

function EntityMode:hideGUI()

  for k, v in pairs(self.widgets) do
    v.window:hide()
  end

end

function EntityMode:activate()
  self.level = getCurState():getLevel()
  self:showGUI()
end

function EntityMode:deactivate()
  self:hideGUI()
end

function EntityMode:updateLevel(level)
  self.level = level
end

function EntityMode:onMouseLeft(down)


  if down then

    local game = getCurState()
    local levelEditor = self.levelEditor

    local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()

    if self.levelEditor.activeLayer == levelEditor.FOREGROUND then
      mouseX, mouseY = game:getForegroundLayer():wndToWorld( mouseX, mouseY )
    else
      mouseX, mouseY = game:getBackgroundLayer():wndToWorld( mouseX, mouseY )
    end


    local found = false

    if self.levelEditor.activeLayer == levelEditor.FOREGROUND then

    for name, entity in pairs(self.level:getEntities()) do
      if entity:inside(mouseX, mouseY) then
        self.selectedEntity = entity
        found = true
        break
      end
    end
    else -- background layer

    for name, entity in pairs(self.level:getEntities()) do
      if entity:inside(mouseX, mouseY) then
        self.selectedEntity = entity
        found = true
        break
      end
    end

    end

      if not found then
        self.selectedEntity = nil
        self:hideImageGUI()
      else
        self:showImageGUI()
      end

  end

end

function EntityMode:onMouseRight(down)

  local game = getCurState()

  if self.selectedEntity ~= nil then

    if down then
      self.moveEntity = true

      local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()
      mouseX, mouseY = game:getForegroundLayer():wndToWorld( mouseX, mouseY )
      local x,y = self.selectedEntity:getPosition()
      self.offsetX = x - mouseX
      self.offsetY = y - mouseY

    else
      self.moveEntity = false
    end

  end

end

function EntityMode:onMouseMove()

  local game = getCurState()
  local levelEditor = self.levelEditor

  local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()

  if self.levelEditor.activeLayer == levelEditor.FOREGROUND then
    mouseX, mouseY = game:getForegroundLayer():wndToWorld( mouseX, mouseY )
  else
    mouseX, mouseY = game:getBackgroundLayer():wndToWorld( mouseX, mouseY )
  end
  if self.selectedEntity ~= nil and self.moveEntity then
   self.selectedEntity:setPosition(mouseX + self.offsetX, mouseY + self.offsetY)
  end

end

function EntityMode:onKeyDown(keyCode, down)

  if keyCode == 127 and down then -- DEL
    self:removeSelectedEntity()
  end

end

function EntityMode:removeSelectedEntity()

  if self.selectedEntity ~= nil then
    self:onRemove(self.selectedEntity)
    self.level:removeEntity(self.selectedEntity:getName())
    self.selectedEntity:destroy()
    self.selectedEntity = nil
    self:hideImageGUI()
  end

end

function EntityMode:onDraw()

  self:drawEntityMarkers()
end

function EntityMode:drawEntityMarkers()

  local selectedEntity = self.selectedEntity
  local game = getCurState()

  for name, entity in pairs(self.level:getEntities()) do
    local x, y = entity:getPosition()

    if self.selectedEntity == entity then
      MOAIGfxDevice.setPenColor(1,0,0)
    else
      MOAIGfxDevice.setPenColor(1,1,1)
    end
    MOAIDraw.drawCircle(x, y, 0.5, 32)
  end

end

function EntityMode:clear()
  local gui = getCurState():getGUI()

  for k, v in pairs(self.widgets) do
    gui:destroyWindow(v.window)
  end
end

function EntityMode:save()

  local file = io.open("lua/game/levels/" .. self.levelEditor:getCurLevel() .. "/entities.lua", "w")
  local piece
  local x, y

  file:write("local numEntities = " .. self.level:getEntityCount() .. "\n\n")

  file:write("local entities = {\n")

  local i = 1

  for name, entity in pairs(self.level:getEntities()) do

    file:write("  " .. name .. " = {\n")

    file:write("    class = \"" .. entity:getClassName() .. "\",\n")
    x, y = entity:getPosition()
    file:write("    location = {" .. x .. ", " .. y .. "}")
    
    if entity:instanceOf(PipeLine) then
      
      file:write(",\n")
      local start, count, directions, types = entity.creator:getSerializeData()
      local scale = entity.creator:getScale()
      
      file:write("    start = " .. start .. ",\n")
      file:write("    scale = " .. scale .. ",\n")
      file:write("    count = " .. count .. ",\n")
      file:write("    directions = {\n")
  
      for i = 1, count do
        if i < count then 
          file:write("      [" .. i .. "] = " ..  directions[i] .. ",\n")
        else
          file:write("      [" .. i .. "] = " ..  directions[i] .. "\n")
        end
      end
  
      file:write("    },\n")
      
      file:write("    types = {\n")
      
      for i = 1, #types do
        if i < #types then 
          file:write("      [" .. i .. "] = " ..  types[i] .. ",\n")
        else
          file:write("      [" .. i .. "] = " ..  types[i] .. "\n")
        end
      end
      
      file:write("    }\n")
      
    else
      file:write("\n")  
    end

    if i < self.level:getEntityCount() then
     file:write("  },\n")
    else
      file:write("  }    \n\n")
    end

    i = i + 1
  end

  file:write("}\n\nreturn numEntities, entities\n")
  io.close(file);

end

function EntityMode:checkForSpecialEntities(entity)

  if entity:instanceOf(PipeLine) then
    self.pipeCreatorGUI = PipeCreatorGUI.new()
    self.pipeCreatorGUI:setPipeLine(entity)
    self.levelEditor:showPipeLineEditDialog()
  end

end

function EntityMode:createPipeGUI(dialogWidgets)
  self.pipeCreatorGUI:createGUI(dialogWidgets)
end

function EntityMode:isCanvasLockedToForeground()
  return true
end

function EntityMode:onRemove(entity)

  if entity:instanceOf(PipeLine) then
    if self.pipeCreatorGUI then
      self.pipeCreatorGUI:clearGUI()
    end
    self.pipeCreatorGUI = nil
  end

end

return EntityMode
