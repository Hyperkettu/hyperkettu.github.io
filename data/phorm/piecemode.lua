local EditorMode = require "game.leveleditor.editormode"
local util = require "core.util"
local Piece = require "game.entities.piece"
local Prefab = require "game.entities.prefab"

-- Piece mode -- TODO  hierarchical move , duplicate piece

local PieceMode = class(EditorMode, "PieceMode")

PieceMode.DEFAULT_IMAGE_SCALE = 1 / 100

function PieceMode:init(levelEditor)
  EditorMode.init(self, levelEditor)

  local game = getCurState()
  self.level = game:getLevel()

  self.fileList = MOAIFileSystem.listFiles("textures/" .. self.levelEditor:getCurLevel() .. "_level")
  self.path = "textures/" .. self.levelEditor:getCurLevel() .. "_level"

  self.selectedImage = nil
  self.selectIndex = 1
  self.imageRotation = 0
  self.imageScale = self.DEFAULT_IMAGE_SCALE
  self.positionX = 0
  self.positionY = 0

  self.DEFAULT_PRIORITY = 0

  self.TRANSLATE = 1
  self.ROTATE = 2
  self.SCALE = 3

  self.hierarchy = false
  self.hierarchyRemove = false

  self.transformMode = self.TRANSLATE
  self.shouldRotate = false
  self.shouldScale = false
  self.shouldTranslate = false

  self.selectPivot = false

  self.currentLayer = self.levelEditor:getActiveLayerIndex()

  self.prefab = nil

  -- Load a font
  local charcodes = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?()&/-'
  local font = MOAIFont.new()
  font:loadFromTTF( 'arial-rounded.TTF', charcodes, 16, 72 )

  local hudLayer = game:getHUDLayer()

  -- Create text boxes
  self.textboxes = {}
  local yOffset = 20
  for i = 1, 1 do
    local textbox = MOAITextBox.new ()
    textbox:setFont( font )
    textbox:setTextSize( 16, 72 )
    textbox:setAlignment ( MOAITextBox.LEFT_JUSTIFY, MOAITextBox.RIGHT_JUSTIFY )
    textbox:setColor( 1, 0, 0 )
    local x, y = hudLayer:wndToWorld( windowWidth * 2 / 3, yOffset )
    local x2, y2 = hudLayer:wndToWorld( windowWidth, windowHeight )
    textbox:setRect( x, y, x2, y2 )
    textbox:setYFlip( true )
    textbox:setVisible( false )
    hudLayer:insertProp( textbox )
    self.textboxes[i] = textbox
    yOffset = yOffset + 20
  end

  self:createGUI()

end

function PieceMode:clear()
  getCurState():getHUDLayer():removeProp(self.textboxes[1])
  self:clearGUI()
end

function PieceMode:activate()
  self.textboxes[1]:setVisible(true)
  self.fileList = MOAIFileSystem.listFiles(self.path)

  self.parentLabel:show()
  self.duplicateButton:show()
  self.hierButton:show()
  self.remButton:show()
  self.infoLabel:show()
  self.pieceButton:show()
  self.maskButton:show()
  self.editPrefabCheckbox:show()
  self.editPrefabLabel:show()
  self.savePrefabButton:show()
  self.loadPrefabButton:show()
  self.selectPivotButton:show()
  self.cancelAllButton:show()


    if self.prefab ~= nil then
      self:showPrefabData(true)
    end



  self.level = getCurState():getLevel()
  if self.selectedImage ~= nil then
    self:showGUI()
  end
end

function PieceMode:deactivate()
  self.textboxes[1]:setVisible(false)
  self:hideGUI()
end

function PieceMode:layerChanged(newLayerIndex)

  if self.selectedImage ~= nil and newLayerIndex ~= self.selectedImage:getLayerIndex() then

    local game = getCurState()
    local x,y = self.selectedImage:getLoc()
    x,y = game:getLayer(self.selectedImage:getLayerIndex()):worldToWnd(x,y)
    x,y = game:getLayer(newLayerIndex):wndToWorld(x,y)
    self.selectedImage:removePieceFromLayer()
    self.selectedImage:insertToLayer(newLayerIndex)
    self.selectedImage:setLoc(x,y)
  end

  -- if layer really changed
  if newLayerIndex ~= self.currentLayer then
    self.currentLayer = newLayerIndex
  end

end

function PieceMode:updateLevel(level)
  self.level = level

  -- fix some issues
  self.selectedImage = nil
  self.prefab = nil

  self:showPrefabData(false)

end

function PieceMode:cancelAllSelections()
  self.selectedImage = nil
  self.prefab = nil
  self:hideImageGUI()
end

function PieceMode:checkForSpecialCommands(cmd, transform)

  -- check for special commands
  if string.sub(cmd, 1, 2) == "x." then
    local posx = tonumber(string.sub(cmd, 4))
    local x,y = transform:getLoc()
    if transform:instanceOf(Prefab) then
      transform:setLocationEditor(posx, y)
    else
      transform:setLoc(posx, y)
    end
    return true
  elseif string.sub(cmd, 1, 2) == "y." then
    local posy = tonumber(string.sub(cmd, 4))
    local x,y = transform:getLoc()
    if transform:instanceOf(Prefab) then
      transform:setLocationEditor(x, posy)
    else
      transform:setLoc(x, posy)
    end
    return true
  elseif string.sub(cmd, 1, 4) == "rot." then
    local rot = tonumber(string.sub(cmd, 6))
    if transform:instanceOf(Prefab) then
      transform:setRotation(rot)
      transform:forceUpdate()
      self.prefab:updateMask()
    else
      transform:setRot(rot)
    end
    return true
  elseif string.sub(cmd, 1, 2) == "w." then
    local width = tonumber(string.sub(cmd, 4))
    transform:setWidth(width)
    local height = transform:getHeight()
    if transform:hasMask() then
      transform:getMask():setLlc(-width/2, -height/2)
      transform:getMask():setUrc(width/2, height/2)
      if self.prefab ~= nil then
        self.prefab:updateMask()
      end
    end
    return true
  elseif string.sub(cmd, 1, 2) == "h." then
    local height = tonumber(string.sub(cmd, 4))
    transform:setHeight(height)
    local width = transform:getWidth()
    if transform:hasMask() then
      transform:getMask():setLlc(-width/2, -height/2)
      transform:getMask():setUrc(width/2, height/2)
      if self.prefab ~= nil then
        self.prefab:updateMask()
      end
    end
    return true
  elseif string.sub(cmd, 1, 4) == "scl." then
    local scl = tonumber(string.sub(cmd, 6))
    if transform:instanceOf(Prefab) then
      local orig = transform:getScale()
      transform:setScaleRelative(scl/orig)
      transform:forceUpdate()
      self.prefab:updateMask()
    else
      local width = transform:getWidth() * scl
      local height = transform:getHeight() * scl
      transform:setSize(width, height)
    end
    return true
  end

  return false

end

function PieceMode:createGUI()

  local gui = getCurState():getGUI()

  -- Load piece mode layout
  local roots, widgets, groups = gui:loadLayout(self.levelEditor:getResources().getPath("piecemodelayout.lua"), nil, self.levelEditor:getWidgets().bg.window)
  self.widgets = widgets

  self.slider = self.widgets.slider1
  self.label = self.widgets.priorityLabel
  self.nameEdit = self.widgets.nameEdit
  self.nameLabel = self.widgets.nameLabel
  self.positionLabel = self.widgets.positionLabel.window
  self.rotationLabel = self.widgets.rotationLabel.window
  self.scaleLabel = self.widgets.scaleLabel.window

  self.fixedScaleLabel = self.widgets.fixedScaleLabel.window
  self.fixedRotationLabel = self.widgets.fixedRotationLabel.window
  self.fixedScaleCheckbox = self.widgets.fixedScaleCheckbox.window
  self.fixedRotationCheckbox = self.widgets.fixedRotationCheckbox.window


  self.parentLabel = self.widgets.parentLabel.window
  self.hierButton = self.widgets.hierButton.window
  self.duplicateButton = self.widgets.duplicateButton.window
  self.remButton = self.widgets.remButton.window
  self.infoLabel = self.widgets.infoLabel.window
  self.pieceButton = self.widgets.pieceButton.window
  self.maskButton = self.widgets.maskButton.window
  self.editMaskLabel = self.widgets.editMaskLabel.window
  self.editMaskCheckbox = self.widgets.editMaskCheckbox.window
  self.editPrefabCheckbox = self.widgets.editPrefabCheckbox.window
  self.editPrefabLabel = self.widgets.editPrefabLabel.window
  self.savePrefabButton = self.widgets.savePrefabButton.window
  self.loadPrefabButton = self.widgets.loadPrefabButton.window
  self.selectPivotButton = self.widgets.selectPivotButton.window
  self.cancelAllButton = self.widgets.cancelAllButton.window

  local w = self.widgets.nameEdit.window
    w:registerEventHandler(w.EVENT_EDIT_BOX_TEXT_ACCEPTED, nil,
        function()

          if self.widgetList ~= nil then

            local cmd = self.widgets.nameEdit.window:getText()

            if cmd == ".." then

              local path = self.path

              path = string.sub(path, 0, string.len(path) - 1)

                local lastSlash
                local nextSlash = 0
                repeat
                  lastSlash = nextSlash
                  nextSlash = string.find(path, "/", lastSlash + 1)
                until nextSlash == nil
                self.path = string.sub(path, 0, lastSlash)
                self:showAvailableFolders()
            else

              local path = self.path

              local newPath = MOAIFileSystem.listDirectories(path .. cmd)

              -- check if new folder is valid
              if newPath ~= nil then
                self.path = path .. cmd .. "/"
                self:showAvailableFolders()
              end

            end

            self.nameEdit.window:setText("")

            self.fileList = MOAIFileSystem.listFiles(self.path)
            self.dirLabel:setText("Dir:  " .. self.path)

            self.widgetList:clearList()

            if self.fileList == nil then
              return
            end

              for i, v in ipairs(self.fileList) do
                local row = self.widgetList:addRow()
                row:getCell(1):setText(v)
              end

            return

          end

          if self.selectedImage ~= nil then

            local newName = self.widgets.nameEdit.window:getText()

              -- check for special commands
             if self:checkForSpecialCommands(newName, self.selectedImage) then
               self.widgets.nameEdit.window:setText(self.selectedImage:getName())
               return
             end

            -- rename a selected piece in prefab edit mode
            if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then

              if self.prefab:getPiece(newName) == nil then
                self.selectedImage = self.prefab:removePiece(self.selectedImage:getName())
                self.selectedImage:setName(newName)
                self.prefab:addPiece(newName, self.selectedImage)
              else
                print("Error while renaming piece: a piece with the name '" .. newName .. "' already exists!")
              end

            else

              if self.level:getPiece(newName) == nil then
                -- else rename piece normally
                self.selectedImage = self.level:removePiece(self.selectedImage:getName())
                self.selectedImage:setName(newName)
                self.level:addPiece(newName, self.selectedImage)
              else
                print("Error while renaming piece: a piece with the name '" .. newName .. "' already exists!")
              end

            end

         else -- selected image nil

           -- if only prefab is selected in normal or edit mode
           if self.prefab ~= nil then

              local newName = self.widgets.nameEdit.window:getText()

              if self:checkForSpecialCommands(newName, self.prefab) then
                self.widgets.nameEdit.window:setText(self.prefab:getName())
                return
              end

              -- prefab can only be renamed in edit prefab mode
              if self.editPrefabCheckbox:getChecked() then
                self.prefab:setName(newName)
              end

           end

         end

        end)

  self.slider.window:hide()
  self.label.window:hide()
  self.nameEdit.window:hide()
  self.nameLabel.window:hide()
  self.positionLabel:hide()
  self.rotationLabel:hide()
  self.scaleLabel:hide()

  self:showPrefabData(false)

  self.parentLabel:hide()
  self.duplicateButton:hide()
  self.hierButton:hide()
  self.remButton:hide()
  self.infoLabel:hide()
  self.pieceButton:hide()
  self.maskButton:hide()
  self.editMaskCheckbox:hide()
  self.editMaskLabel:hide()
  self.editPrefabCheckbox:hide()
  self.editPrefabLabel:hide()
  self.savePrefabButton:hide()
  self.loadPrefabButton:hide()
  self.selectPivotButton:hide()
  self.cancelAllButton:hide()

  self.fixedScaleCheckbox:registerEventHandler(self.fixedScaleCheckbox.EVENT_CHECK_BOX_STATE_CHANGE, nil, function()

    if self.prefab ~= nil then
      self.prefab:setUniqueScale(self.fixedScaleCheckbox:getChecked())
    end

  end)

  self.fixedRotationCheckbox:registerEventHandler(self.fixedRotationCheckbox.EVENT_CHECK_BOX_STATE_CHANGE, nil, function()

    if self.prefab ~= nil then
      self.prefab:setUniqueRotation(self.fixedRotationCheckbox:getChecked())
    end

  end)


  self.cancelAllButton:registerEventHandler(self.cancelAllButton.EVENT_BUTTON_CLICK, nil, function()

    self:cancelAllSelections()

  end)


  self.selectPivotButton:registerEventHandler(self.selectPivotButton.EVENT_BUTTON_CLICK, nil, function()
   self.selectPivot = true
  end)

  self.savePrefabButton:registerEventHandler(self.savePrefabButton.EVENT_BUTTON_CLICK, nil, function()

    if self.prefab ~= nil and self.editPrefabCheckbox:getChecked() then
      self.prefab:save()
      self.prefab = nil
      self:hideImageGUI()
    end

  end)

  self.loadPrefabButton:registerEventHandler(self.loadPrefabButton.EVENT_BUTTON_CLICK, nil, function()

    local path = "data/prefabs"
    self.tempPath = self.path
    self.levelEditor:showFileListDialog(path, function(p,i) self:handlePrefabSelect(p,i) end)

  end)

  self.duplicateButton:registerEventHandler(self.duplicateButton.EVENT_BUTTON_CLICK, nil, function()
   self:duplicate()
  end)

  self.hierButton:registerEventHandler(self.hierButton.EVENT_BUTTON_CLICK, nil, function()
      self.hierarchy = true
      self.selectedImage = nil
      self.parentLabel:setText("On\n")
      self:hideImageGUI()
      self.infoLabel:setText("Select Parent")
end)

    self.remButton:registerEventHandler(self.remButton.EVENT_BUTTON_CLICK, nil, function()
    self.hierarchyRemove = true
    self.parentLabel:setText("On (rm)")
    self:hideImageGUI()
    self.selectedImage = nil
    self.infoLabel:setText("Select Parent")
end)

  w = self.slider.window
  w:registerEventHandler(w.EVENT_SLIDER_VALUE_CHANGED, nil,
        function()
           self:updatePriority()
        end)

  local pieceButton = self.widgets.pieceButton.window
  pieceButton:registerEventHandler(pieceButton.EVENT_BUTTON_CLICK, nil, function()

      local path = self.path
      self.levelEditor:showFileListDialog(path, function(p,i) self:handlePieceSelect(p,i) end)

end)

  self.maskButton:registerEventHandler(self.maskButton.EVENT_BUTTON_CLICK, nil, function()

      self.tempPath = self.path
      local path = "textures/masks"
      self.levelEditor:showFileListDialog(path, function(p,i) self:handleMaskSelect(p,i) end)

  end)

end

function PieceMode:clearGUI()

  local gui = getCurState():getGUI()

  for k, v in pairs(self.widgets) do
    gui:destroyWindow(v.window)
  end

end

function PieceMode:showGUI()

  if self.slider ~= nil and self.label ~= nil and self.nameEdit ~= nil and self.nameLabel ~= nil then

    -- found prefab and no selected image either in edit prefab and normal mode
    if self.prefab ~= nil and self.selectedImage == nil then

      self:showPrefabData(true)

    else -- no prefab selected



    self.slider.window:show()
    self.nameEdit.window:show()
    self.nameLabel.window:show()
    self.positionLabel:show()
    self.rotationLabel:show()
    self.scaleLabel:show()
    self.pieceButton:show()
    self.maskButton:show()

    if self.selectedImage ~= nil then
      self.slider.window:setCurrValue(self.selectedImage:getPriority())

      if self.selectedImage:getName() ~= nil then
        self.nameLabel.window:setText("Image name:")
        self.nameEdit.window:setText(self.selectedImage:getName())
      else
        self.nameEdit.window:setText("")
      end

      if self.selectedImage:hasMask() then
        self.editMaskLabel:show()
        self.editMaskCheckbox:show()
      end

    end
    self.label.window:show()

  end

  end

end

function PieceMode:hideGUI()

  if self.slider ~= nil and self.label ~= nil and self.nameEdit ~= nil and self.nameLabel ~= nil then

    self.slider.window:hide()
    self.label.window:hide()
    self.nameEdit.window:hide()
    self.nameLabel.window:hide()
    self.positionLabel:hide()
    self.rotationLabel:hide()

    self.parentLabel:hide()
    self.duplicateButton:hide()
    self.hierButton:hide()
    self.remButton:hide()
    self.infoLabel:hide()
    self.pieceButton:hide()
    self.maskButton:hide()
    self.editPrefabCheckbox:hide()
    self.editPrefabLabel:hide()
    self.savePrefabButton:hide()
    self.loadPrefabButton:hide()
    self.selectPivotButton:hide()
    self.cancelAllButton:hide()

    self:showPrefabData(false)

  end

end

function PieceMode:hideImageGUI()

  if self.slider ~= nil and self.label ~= nil and self.nameEdit ~= nil and self.nameLabel ~= nil then

    self.slider.window:hide()
    self.label.window:hide()
    self.nameEdit.window:hide()
    self.nameLabel.window:hide()
    self.positionLabel:hide()
    self.rotationLabel:hide()
    self.editMaskLabel:hide()
    self.editMaskCheckbox:hide()


    self:showPrefabData(false)
    -- if selected image data is hidden when prefab is selected
    -- show prefab data
    if self.editPrefabCheckbox:getChecked() then

      if self.prefab ~= nil then
        self:showPrefabData(true)
      end

    end

  end

end

function PieceMode:showPrefabData(show)

  if show then
    -- show rotation, scale
    self.fixedScaleLabel:show()
    self.fixedRotationLabel:show()
    self.fixedScaleCheckbox:show()
    self.fixedRotationCheckbox:show()

    self.positionLabel:show()
    self.rotationLabel:show()
    self.scaleLabel:show()


    self.nameEdit.window:show()
    self.nameLabel.window:show()
    self.nameLabel.window:setText("Prefab name:")
    self.nameEdit.window:setText(self.prefab:getName())

    if self.prefab.hasUniqueScale then
      self.fixedScaleCheckbox:setChecked(true)
    else
      self.fixedScaleCheckbox:setChecked(false)
    end

    if self.prefab.hasUniqueRotation then
      self.fixedRotationCheckbox:setChecked(true)
    else
      self.fixedRotationCheckbox:setChecked(false)
    end

  else -- hide

    self.fixedScaleLabel:hide()
    self.fixedRotationLabel:hide()
    self.fixedScaleCheckbox:hide()
    self.fixedRotationCheckbox:hide()

    self.positionLabel:hide()
    self.rotationLabel:hide()
    self.scaleLabel:hide()

  end

end

function PieceMode:hideOtherLayerPieces(currentLayer)

  for name, piece in pairs(self.level:getPieces()) do
    if piece:getLayerIndex() ~= currentLayer then
      piece:setVisible(false)
    end
  end

end

function PieceMode:showAllPieces()

  for name, piece in pairs(self.level:getPieces()) do
      piece:setVisible(true)
  end

end

function PieceMode:updatePriority()

  if self.selectedImage ~= nil then
    self.selectedImage:setPriority(self.slider.window:getCurrValue())
  end

end

function PieceMode:parent(piece)

  if self.hierarchy then
    if self.selectedImage ~= nil then
      piece:addChild(self.selectedImage)
      self.hierarchy = false
      self.parentLabel:setText("Off")
      self.infoLabel:setText("")
    else
      -- sel image nil
       self.infoLabel:setText("Select Parent")
    end

  elseif self.hierarchyRemove then
    if self.selectedImage ~= nil then
      self.selectedImage:removeChild(piece)
      self.hierarchyRemove = false
      self.parentLabel:setText("Off")
      self.infoLabel:setText("")
    else
      self.infoLabel:setText("Select Child")
    end
  end

end

-- de/select image on click
function PieceMode:onMouseLeft(down)

  if down then

    local game = getCurState()
    local levelEditor = self.levelEditor
    local partition = game:getForegroundLayer():getPartition()
    local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()

    mouseX, mouseY = game:getLayer(self.levelEditor:getActiveLayerIndex()):wndToWorld( mouseX, mouseY )

    local found = false
    local priorityMin = -9999

    -- in prefab edit mode can select pivot for
    if self.editPrefabCheckbox:getChecked() then

    if self.prefab ~= nil then

        if self.selectPivot and self.selectedImage ~= nil then
        --  local dx, dy = self.prefab:getPivotWorldPosition(self.selectedImage)
        --  local px, py = self.selectedImage:getPiv()
        --  dx = mouseX - dx + px
        --  dy = mouseY - dy + py
          local dx, dy = self.selectedImage:worldToModel(mouseX, mouseY)
          self.selectedImage:setPiv(dx, dy)
          print(dx .. ", " .. dy)
          self.selectPivot = false
          return
        end

        -- find piece in prefab in edit mode
        for name, piece in pairs(self.prefab:getPieces()) do
          if piece:inside(mouseX, mouseY, self.levelEditor:getActiveLayerIndex()) and piece:getPriority() > priorityMin then
             self.selectedImage = piece
             priorityMin = piece:getPriority()
             found = true
          end
        end
    end

    else -- not checked i.e. normal mode so search for pieces and prefabs

      local selectedPiece

      for name, piece in pairs(self.level:getPieces()) do

        if piece:inside(mouseX, mouseY, self.levelEditor:getActiveLayerIndex()) and piece:getPriority() > priorityMin then
           selectedPiece = piece

           self.prefab = nil
           priorityMin = piece:getPriority()
           found = true
        end
      end

    self:parent(selectedPiece)
    self.selectedImage = selectedPiece

    end

      --search for prefab in both cases edit and normal mode
      for name, prefabInstance in pairs(self.level:getPrefabInstances()) do

        -- for each prefab check for pieces similarly as normal pieces (priority)
        for pieceName, piece in pairs(prefabInstance:getPieces()) do

          if piece:inside(mouseX, mouseY, self.levelEditor:getActiveLayerIndex()) and piece:getPriority() > priorityMin then

             self.prefab = prefabInstance
             self.selectedImage = nil
             priorityMin = piece:getPriority()
             found = true
          end
        end

      end

    if not found then
      self.selectedImage = nil
      self.prefab = nil
      self:hideImageGUI()
    else
      self:hideImageGUI()
      self:showGUI()
    end

  end

end

function PieceMode:onMouseRight(down)

  local game = getCurState()
  local levelEditor = self.levelEditor
  local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()

  if down then

    mouseX, mouseY = game:getLayer(self.levelEditor:getActiveLayerIndex()):wndToWorld( mouseX, mouseY )

    self.x = mouseX
    self.y = mouseY

    -- check if mask is edited
    if self.editMaskCheckbox:getChecked() then
      self.editMask = true
      return
    end

    -- start rotating on mouse down
    if self.transformMode == self.ROTATE then
      self.shouldRotate = true
    end

    if self.transformMode == self.SCALE then

      local x, y
      -- if selected image
      if self.selectedImage ~= nil then
        -- edit scale selectedImage in edit prefab mode
        if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
          x, y = self.prefab:getWorldPosition(self.selectedImage)
        else
          x, y = self.selectedImage:getLoc()
        end
      else -- no selected image, scale prefab in edit and normal mode
        if self.prefab ~= nil then
          x,y = self.prefab:getLoc()
        end
      end

      local mouseToPieceDist = util.distance(x,y, mouseX, mouseY)
      self.shouldScale = true
      self.scalePrevDist = mouseToPieceDist
    end

    if self.transformMode == self.TRANSLATE then
      self.shouldTranslate = true
      if self.selectedImage ~= nil then
        local x, y = self.selectedImage:getLoc()
        self.offsetX = x - mouseX
        self.offsetY = y - mouseY

        -- offset in world
        if self.prefab ~= nil then
          x,y = self.selectedImage:modelToWorld()
          self.offsetX = x - mouseX
          self.offsetY = y - mouseY
        end

      else -- selected image is nil

        -- translate prefab in both edit and normal mode
        if self.prefab ~= nil then
          local x, y = self.prefab:getLoc()
          self.offsetX = x - mouseX
          self.offsetY = y - mouseY
        end

      end
    end

  else -- mouse up

    mouseX, mouseY = game:getLayer(self.levelEditor:getActiveLayerIndex()):wndToWorld( mouseX, mouseY )

    -- stop editing
    if self.editMaskCheckbox:getChecked() then
      self.editMask = false
      return
    end

    -- end moving prefabInstance if no selected image in edit and normal mode
    if self.prefab ~= nil then
      if self.selectedImage == nil then
        self.shouldTranslate = false
      end
    end

    -- set selected image location
    if self.transformMode == self.TRANSLATE then
      self.shouldTranslate = false
      if self.selectedImage ~= nil then

        if self.prefab ~= nil then
          local x,y = mouseX + self.offsetX, mouseY + self.offsetY
          x,y = self.prefab:worldToModel(x,y)
          self.selectedImage:setLocEditor(x, y)
        else
          self.selectedImage:setLocEditor(mouseX + self.offsetX, mouseY + self.offsetY)
        end

      end
    -- end rotating on mouse up
    elseif self.transformMode == self.ROTATE then
      if self.selectedImage ~= nil then

        local x, y

        -- end edit image rotation in edit prefab mode and normal mode
        if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
          x, y = self.prefab:getPivotWorldPosition(self.selectedImage)
        else
          x, y = self.selectedImage:getLoc()
        end
      --  print(x .. ", " .. y)
        local angle = util.calculateAngle(x, y, self.x, self.y, mouseX, mouseY)
        self.selectedImage:setRot(self.imageRotation + angle)
        self.imageRotation = self.selectedImage:getRot()
      end
      self.shouldRotate = false
    elseif self.transformMode == self.SCALE then
      self.shouldScale = false
    end

  end

end

function PieceMode:onMouseMove()

  local game = getCurState()
  local levelEditor = self.levelEditor

  local mouseX, mouseY = MOAIInputMgr.device.pointer:getLoc()

  mouseX, mouseY = game:getLayer(self.levelEditor:getActiveLayerIndex()):wndToWorld( mouseX, mouseY )

  if self.editMask and self.selectedImage ~= nil and self.selectedImage:getMask() ~= nil then

    if self.prefab ~= nil then

      local mask = self.selectedImage:getMask()
      local x, y = mask:getUrcValues()
      x,y = self.prefab:modelToWorld(x,y)

      if util.distance(x, y, mouseX, mouseY) < 0.5 then
        mouseX, mouseY = self.prefab:worldToModel(mouseX, mouseY)
        mask:setUrc(mouseX, mouseY)
        mask:updateBorders(self.prefab:getBaseTransform(), self.selectedImage)
        return
      end

      x, y = mask:getLlcValues()
      x,y = self.prefab:modelToWorld(x,y)

      if util.distance(x, y, mouseX, mouseY) < 0.5 then
        mouseX, mouseY = self.prefab:worldToModel(mouseX, mouseY)
        mask:setLlc(mouseX, mouseY)
        mask:updateBorders(self.prefab:getBaseTransform(), self.selectedImage)
        return
      end

    end

    return

  end

    -- move prefabInstance if no selected image
    if self.prefab ~= nil then
      if self.selectedImage == nil and self.shouldTranslate then
        self.prefab:setLocationEditor(mouseX + self.offsetX, mouseY + self.offsetY)
      end
    end

  if self.transformMode == self.TRANSLATE then
    if self.selectedImage ~= nil and self.shouldTranslate then

      if self.prefab ~= nil then
        local x,y = mouseX + self.offsetX, mouseY + self.offsetY
        x,y = self.prefab:worldToModel(x,y)
        self.selectedImage:setLocEditor(x,y)
      else
        self.selectedImage:setLocEditor(mouseX + self.offsetX, mouseY + self.offsetY)
      end
    end
  elseif self.transformMode == self.ROTATE then

    -- rotate prefab in any case
    if self.prefab ~= nil then

      -- rotate prefab if no selected image
      if self.selectedImage == nil and self.shouldRotate then

        local x, y = self.prefab:getLoc()
        self.prefab:setRotation()
        local angle = util.calculateAngle(x, y, self.x, self.y, mouseX, mouseY)
        self.prefab:setRotation(self.imageRotation + angle)

      end

    end

  -- if image is selected rotate
    if self.selectedImage ~= nil and self.shouldRotate then

      local x, y

      if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
        x, y = self.prefab:getPivotWorldPosition(self.selectedImage)
      else
        x, y = self.selectedImage:getLoc()
      end

      local angle = util.calculateAngle(x, y, self.x, self.y, mouseX, mouseY)
      self.selectedImage:setRot(self.imageRotation + angle)
    end
  elseif self.transformMode == self.SCALE then

    -- scale prefab in edit or normal mode
    if self.prefab ~= nil then
      -- if no selected image scale prefab instead of image
      if self.selectedImage == nil and self.shouldScale then

        local x,y = self.prefab:getLoc()
        local mouseToPieceDist = util.distance(x,y, mouseX, mouseY)

        local scale = mouseToPieceDist / self.scalePrevDist

        self.scalePrevDist = mouseToPieceDist
        self.prefab:setScaleRelative(scale)

      end
    end

    if self.selectedImage ~= nil and self.shouldScale then

      local x, y

      if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
        x, y = self.prefab:getWorldPosition(self.selectedImage)
      else
        x, y = self.selectedImage:getLoc()
      end

      local mouseToPieceDist = util.distance(x,y, mouseX, mouseY)
      local width, height = self.selectedImage:getSize()

      local scale = mouseToPieceDist / self.scalePrevDist

      self.scalePrevDist = mouseToPieceDist

      self.selectedImage:setSize(width * scale, height * scale)

    end

  end

end

function PieceMode:onKeyDown(keyCode, down)

  if keyCode >= 256 then return end

  if string.char(keyCode) == 'r' and down then

    if self.selectedImage == nil then
        self:previousIndex()
        self:insertImageToLayer()
    else
      self:changeSelectedTexturePrev()
    end

  elseif string.char(keyCode) == 't' and down then

     if self.selectedImage == nil then
        self:nextIndex()
        self:insertImageToLayer()
    else
      self:changeSelectedTextureNext()
    end
    -- tab pressed changes the transform mode to SCALE/ROTATE
   elseif string.char(keyCode) == '\t' and down then

      if self.transformMode == self.TRANSLATE then
        self.transformMode = self.ROTATE
      elseif self.transformMode == self.ROTATE then
        self.transformMode = self.SCALE
      elseif self.transformMode == self.SCALE then
        self.transformMode = self.TRANSLATE
      end
    elseif keyCode == 4 and down then -- ctrl + d
      self:duplicate()
    elseif string.char(keyCode) == 'h' and down then
      self.hierarchy = true
      self.parentLabel:setText("On")
      self.selectedImage = nil
      self.infoLabel:setText("Select Child")
    elseif string.char(keyCode) == 'r' and down then
      self.hierarchyRemove = true
      self.parentLabel:setText("On (rm)")
      self.selectedImage = nil
      self.infoLabel:setText("Select Parent")
    elseif keyCode == 127 and down then -- DEL
       self:removeSelectedImage()
    elseif string.char(keyCode) == 'q' and down then -- . to deselect
      self:cancelAllSelections()
    elseif string.char(keyCode) == '5' and down then
      self:createPipeLine()
    elseif string.char(keyCode) == 'S' and down then
      self:save()
  end

end

function PieceMode:onDraw()

  self:updateModeTextbox()
  self:drawPieceBounds()
  self:drawSelectedImageData()
  self:drawSelectedPrefabData()

end

function PieceMode:drawSelectedImageData()

  local selectedImage = self.selectedImage

  if selectedImage ~= nil then
    local x,y = selectedImage:getLoc()
    local w = selectedImage:getWidth()
    local h = selectedImage:getHeight()

    -- scale dimension if prefab is selected
    if self.prefab ~= nil then

      local scale = self.prefab:getScale()
     -- w = w * scale
     -- h = h * scale

     -- x = x * scale
     -- y = y * scale

    end
    x = math.floor( x*100 + 0.5 ) / 100
    y = math.floor( y*100 + 0.5 ) / 100
    w = math.floor( w*100 + 0.5 ) / 100
    h = math.floor( h*100 + 0.5 ) / 100

    local rot = selectedImage:getRot()
    rot = math.floor( rot*100 + 0.5 ) / 100
    self.positionLabel:setText("Position: " .. x .. ", " .. y)
    self.rotationLabel:setText("Rotation: " .. rot)
    self.scaleLabel:setText("Width, Height: " .. w .. ", " .. h )
  end

end

function PieceMode:drawSelectedPrefabData()

  local prefab = self.prefab
  local selectedImage = self.selectedImage

  if prefab ~= nil and selectedImage == nil then

    local x,y = prefab:getLoc()
    x = math.floor( x*100 + 0.5 ) / 100
    y = math.floor( y*100 + 0.5 ) / 100
    local scale = prefab:getScale()
    scale = math.floor( scale*100 + 0.5 ) / 100
    local rot = prefab:getRotation()
    rot = math.floor( rot*100 + 0.5 ) / 100

    self.positionLabel:setText("Position: " .. x .. ", " .. y)
    self.rotationLabel:setText("Rotation: " .. rot)
    self.scaleLabel:setText("Scale: " .. scale )


  end

end

function PieceMode:updateModeTextbox()
  if self.transformMode == self.TRANSLATE then
    self.textboxes[1]:setString("Current mode: Translate (TAB)")
  elseif self.transformMode == self.ROTATE then
    self.textboxes[1]:setString("Current mode: Rotate    (TAB)")
  else
    self.textboxes[1]:setString("Current mode: Scale     (TAB)")
  end
end

function PieceMode:nextIndex()

  if not self.fileList then
    return
  end

  if self.fileList[self.selectIndex + 1] ~= nil then
  self.selectIndex = self.selectIndex + 1
  else
  self.selectIndex = 1
  end

end

function PieceMode:previousIndex()

  if not self.fileList then
    return
  end

  if self.selectIndex - 1 > 0 then
  self.selectIndex = self.selectIndex - 1
  end

end

function PieceMode:changeSelectedTexturePrev()

  self:previousIndex()
  local path = self.path .. "/" .. self.fileList[self.selectIndex]
  self.selectedImage:changeTexture(path)
end

function PieceMode:changeSelectedTextureNext()

  self:nextIndex()
  local path = self.path .. "/" .. self.fileList[self.selectIndex]
  self.selectedImage:changeTexture(path)

end

function PieceMode:insertImageToLayer()

  if not self.fileList then
    return
  end

  local levelEditor = self.levelEditor
  local path = self.path .. "/" .. self.fileList[self.selectIndex]

  -- init piece structure
  local data = {}

  data.fileName = path
 -- data.location = {self.positionX, self.positionY}
  local x,y = getCurState():getCamera():getLoc()
  local layer =  getCurState():getLayer(self.levelEditor:getActiveLayerIndex())
  local foregroundLayer = getCurState():getForegroundLayer()

  x,y = layer:wndToWorld(foregroundLayer:worldToWnd(x, y))
  data.location = {x,y}
  data.rotation = self.imageRotation
  data.priority = self.DEFAULT_PRIORITY
  data.layerIndex = self.levelEditor:getActiveLayerIndex()

  local p -- piece

  local name = self:getFreeNameForPiece("piece") -- FIXME
  local p = Piece.new(name, data)

  -- add to prefab
  if self.editPrefabCheckbox:getChecked() then

    if self.prefab == nil then
      self.prefab = Prefab.new()
      self.prefab:setLocationEditor(x, y)
      self.prefab:forceUpdate()
      local instanceName = "instance" .. self.level:getPrefabInstanceCount()
      self.level:addPrefabInstance(instanceName, self.prefab)
    end

   -- local name = "pic" .. self.prefab:getPieceCount()
   -- p = Piece.new(name, data)
    p:setLoc(0, 0)
    self.prefab:addPiece(name, p)

  else
  --  local name = "pic" .. self.level:getPieceCount()
  --  p = Piece.new(name, data)
    self.level:addPiece(name, p)
  end

  self.selectedImage = p
  self:showGUI()

end

function PieceMode:getFreeNameForPiece(name)
  local game = getCurState()
  local level = game:getLevel()

  if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
    if self.prefab:getPiece(name) == nil then return name end
  else
    if level:getPiece(name) == nil then return name end
  end

  local pos = name:len()
  local n = 2
  local baseName = name

  while pos > 0 do
    local subStr = name:sub(pos)
    local m = tonumber(subStr)
    if m == nil then
      break
    else
      n = m + 1
      baseName = name:sub(1, pos - 1)
    end
    pos = pos - 1
  end

  while true do
    local myName = baseName .. n
    if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
      if self.prefab:getPiece(myName) == nil then
        return myName
      end
    else
      if level:getPiece(myName) == nil then
        return myName
      end
    end
    n = n + 1
  end
end

function PieceMode:duplicate()

  local sel = self.selectedImage

  if sel == nil then

    -- duplicate prefab
    if self.prefab ~= nil then

      local prefabName = self.prefab:getName()
      local x,y = self.prefab:getLoc()
      local scale = self.prefab:getScale()
      local rot = self.prefab:getRotation()

      local prefab = Prefab.instantiate(prefabName, x, y)
      prefab:setScale(scale)
      prefab:setRotation(rot)
      local instanceName = "prefabInstance" .. self.level:getPrefabInstanceCount()
      self.level:addPrefabInstance(instanceName, prefab)
      self.prefab = prefab
    end

  elseif sel ~= nil then

  local data = {}

  data.fileName = sel:getFileName()
  local x,y = sel:getLoc()
  data.location = {x,y}
  data.rotation = sel:getRot()
  data.width = sel:getWidth()
  data.height = sel:getHeight()
  data.priority = sel:getPriority()
  data.layerIndex = sel:getLayerIndex()


  local i = 1

  local name = self:getFreeNameForPiece(sel:getName())
  local piece = Piece.new(name, data)

  local res

  if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
    res = self.prefab:addPiece(name, piece)
  else
    res = self.level:addPiece(name, piece)
  end

  if sel:getChildren() ~= nil then

    for name, p in pairs(sel:getChildren()) do

      self:duplicateChild(p, piece)

    end

  end

  self.selectedImage = piece
  self:showGUI()

  end

end

function PieceMode:duplicateChild(child, parent)

  local sel = child

  if sel ~= nil then

  local data = {}

  data.fileName = sel:getFileName()
  local x,y = sel:getLoc()
  data.location = {x,y}
  data.rotation = sel:getRot()
  data.width = sel:getWidth()
  data.height = sel:getHeight()
  data.priority = sel:getPriority()
  data.layerIndex = sel:getLayerIndex()


  local i = 1

  local name = self:getFreeNameForPiece(sel:getName())
  local piece = Piece.new(name, data)

  local res

  if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
    res = self.prefab:addPiece(name, piece)
  else
    res = self.level:addPiece(name, piece)
  end

  parent:addChild(piece)

  if sel:getChildren() ~= nil then

    for name, p in pairs(sel:getChildren()) do

      self:duplicateChild(p, piece)

    end

  end

  end

end


function PieceMode:drawPieceBounds()

  local selectedImage = self.selectedImage
  local game = getCurState()

  local activeLayer = self.levelEditor:getActiveLayerIndex()

  for name, piece in pairs(self.level:getPieces()) do

    local layerIndex = piece:getLayerIndex()
    local layer = game:getLayer(layerIndex)
    local w2 = piece:getWidth()/2
    local h2 = piece:getHeight()/2

    if activeLayer == layerIndex then

      local x1, y1 = piece:modelToWorld(-w2,-h2)
      local x2, y2 = piece:modelToWorld(w2,-h2)
      local x3, y3 = piece:modelToWorld(w2,h2)
      local x4, y4 = piece:modelToWorld(-w2,h2)

      local points = {x1, y1, x2, y2, x3, y3, x4, y4, x1, y1}

      if self.selectedImage == piece then
        MOAIGfxDevice.setPenColor(1,0,0)
      else
        MOAIGfxDevice.setPenColor(1,1,1)
      end
      MOAIDraw.drawLine(points)

      if self.selectedImage ~= nil and self.selectedImage:hasMask() then
        local mask = self.selectedImage:getMask()
        local x, y = mask:getLlcValues()
        MOAIGfxDevice.setPenColor(0.1,0,0.8)
        MOAIDraw.drawCircle(x,y,0.1, 8)
        x, y = mask:getUrcValues()
        MOAIDraw.drawCircle(x,y,0.1, 8)
      end

    end

  end

  for name, prefab in pairs(self.level:getPrefabInstances()) do

    for k, piece in pairs(prefab:getPieces()) do
      local layerIndex = piece:getLayerIndex()
      local layer = game:getLayer(layerIndex)
      local w2 = piece:getWidth()/2
      local h2 = piece:getHeight()/2

      if activeLayer == layerIndex then

        local x1, y1 = piece:modelToWorld(-w2,-h2)
        local x2, y2 = piece:modelToWorld(w2,-h2)
        local x3, y3 = piece:modelToWorld(w2,h2)
        local x4, y4 = piece:modelToWorld(-w2,h2)

        local points = {x1, y1, x2, y2, x3, y3, x4, y4, x1, y1}

        if self.selectedImage == piece then
          MOAIGfxDevice.setPenColor(1,0,0)
        else
          MOAIGfxDevice.setPenColor(1,1,1)
        end
        MOAIDraw.drawLine(points)

        if self.selectedImage ~= nil and self.selectedImage:hasMask() then
          local mask = self.selectedImage:getMask()
          local x, y = mask:getLlcValues()
          x,y = self.prefab:modelToWorld(x,y)
          MOAIGfxDevice.setPenColor(0.1,0,0.8)
          MOAIDraw.drawCircle(x,y,0.1, 8)
          x, y = mask:getUrcValues()
          x,y = self.prefab:modelToWorld(x,y)
          MOAIDraw.drawCircle(x,y,0.1, 8)
        end
      end
    end

  end

  if selectedImage ~= nil then

    if self.selectPivot then

      if self.prefab ~= nil then

        local x0,y0 = self.prefab:getPivotWorldPosition(selectedImage)
        MOAIGfxDevice.setPenColor(0.1,0,0.8)
        MOAIDraw.drawCircle(x0,y0,0.1, 8)

      end
    end

  end

  if self.prefab ~= nil then
    local x0,y0 = self.prefab:getLoc()
    MOAIGfxDevice.setPenColor(0.0,0.5,0.5)
    MOAIDraw.drawCircle(x0,y0,0.1, 8)

  end

end

function PieceMode:removeSelectedImage()

  if self.selectedImage ~= nil then

    -- remove piece from prefab
    if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then

      self.selectedImage:removePieceFromLayer()
      self.prefab:removePiece(self.selectedImage:getName())
      self.selectedImage = nil
      return

    end

    if self.selectedImage:hasMask() then
      self.editMask = false
    end

    -- remove piece from level
    self.selectedImage:removePieceFromLayer()
    self:hideImageGUI()
    self.level:removePiece(self.selectedImage:getName())
    self.selectedImage = nil

  else
    -- remove prefab in any mode
    if self.prefab ~= nil then

      local name = self.level:findName(self.prefab)

      self.prefab:clean()
      self.level:removePrefabInstance(name)
      self.nameEdit.window:hide()
      self.nameLabel.window:hide()
      self:showPrefabData(false)
      self.prefab = nil

    end

  end

end

function PieceMode:save()
  print("saving")
  local file = io.open("lua/game/levels/" .. self.levelEditor:getCurLevel() .. "/pieces.lua", "w")
  local piece
  local x, y

  file:write("local numPieces = " .. self.level:getPieceCount() .. "\n\n")

  file:write("local pieces = {\n")

  local i = 1

  for name, piece in pairs(self.level:getPieces()) do

    file:write("  " .. name .. " = {\n")

    file:write("    fileName = \"" .. piece.fileName .. "\",\n")
    x, y = piece:getLoc()
    file:write("    location = {" .. string.format("%.3f",x) .. ", " .. string.format("%.3f",y) .. "},\n")
    file:write("    rotation = " .. string.format("%.3f",piece:getRot()) .. ",\n")
    file:write("    width = " .. string.format("%.3f",piece:getWidth()) .. ",\n")
    file:write("    height = " .. string.format("%.3f",piece:getHeight()) .. ",\n")
    file:write("    priority = " .. piece:getPriority() .. ",\n")
    file:write("    layerIndex = " .. piece:getLayerIndex())

    if piece:hasMask() then
      file:write(",\n")
      file:write("    mask = {\n")
      file:write("      fileName = \"" .. piece:getMask():getFileName() .. "\",\n")
      x, y = piece:getMask():getLlcValues()
      file:write("      llc = {" .. string.format("%.3f",x) .. ", " .. string.format("%.3f",y)  .. "},\n")
      x, y = piece:getMask():getUrcValues()
      file:write("      urc = {" .. string.format("%.3f",x) .. ", " .. string.format("%.3f",y)  .. "}\n    }\n")
    else
      file:write("\n")
    end

    if i < self.level:getPieceCount() then
     file:write("  },\n")
    else
      file:write("  }    \n\n")
    end

    i = i + 1
  end

  file:write("}\n\nlocal prefabInstances = {\n")

  i = 1

  for name, prefab in pairs(self.level:getPrefabInstances()) do

    file:write("  " .. name .. " = {\n")
    file:write("    name = \"" .. prefab:getName() .. "\",\n")
    x, y = prefab:getLoc()
    file:write("    location = {" .. string.format("%.3f",x) .. ", " .. string.format("%.3f",y) .. "}")

    if prefab.hasUniqueScale then
      file:write(",\n    scale = " .. string.format("%.3f",prefab:getScale()))
    end

    if prefab.hasUniqueRotation then
      file:write(",\n    rotation = " .. string.format("%.3f",prefab:getRotation()) .. "\n")
    else
      file:write("\n")
    end

    file:write("\n")

    if i < self.level:getPrefabInstanceCount() then
     file:write("  },\n")
    else
      file:write("  }    \n\n")
    end

    i = i + 1
  end

  file:write("}\n\nreturn numPieces, pieces, prefabInstances\n")
  io.close(file);

end

function PieceMode:loadDialogLayout(dialogLayout)
  local game = getCurState()
  local gui = game:getGUI()

  local dialogRoots, dialogWidgets, dialogGroups = gui:loadLayout(self.levelEditor:getResources().getPath(dialogLayout))
  self.dialogWidgets = dialogWidgets

end

function PieceMode:handleMaskSelect(path)

  if self.selectedImage ~= nil then
    if self.selectedImage:hasMask() then
      self.selectedImage:changeMask(path)
    else
      self.selectedImage:createMask(path)

      -- parent is lost due to mask addition so update parent
      if self.editPrefabCheckbox:getChecked() and self.prefab ~= nil then
        self.prefab:updateParent(self.selectedImage)
        local mask = self.selectedImage:getMask()
        self.prefab:updateMask()
      --  local w2, h2 = mask:getUrcValues()
      --  local x,y = self.prefab:getLoc()
      --  mask:setUrc(x + w2, y + h2)
      --  w2, h2 = mask:getLlcValues()
      --  mask:setLlc(x + w2, y + h2)

      end

    end

    self:showGUI()
  end

  self.path = self.tempPath

end

function PieceMode:handlePieceSelect(path, index)

  self.path = util.getDirectoryFromPath(path)
  self.fileList = MOAIFileSystem.listFiles(self.path)
  self.selectIndex = index

  print("ind: " .. index)

  if self.selectedImage == nil then
    self:insertImageToLayer()
  else
    self.selectedImage:changeTexture(path)
  end

end

function PieceMode:handlePrefabSelect(path, index)

  self.path = util.getDirectoryFromPath(path)
  self.fileList = MOAIFileSystem.listFiles(self.path)
  self.selectIndex = index

  local x,y = getCurState():getCamera():getLoc()
  local layer =  getCurState():getLayer(self.levelEditor:getActiveLayerIndex())
  local foregroundLayer = getCurState():getForegroundLayer()
  x,y = layer:wndToWorld(foregroundLayer:worldToWnd(x, y))

  local name = self.fileList[self.selectIndex]
  name = string.sub(name, 1, string.len(name) - 4)
  local prefab = Prefab.instantiate(name, x, y)
  local instanceName = "instance" .. self.level:getPrefabInstanceCount()
  self.level:addPrefabInstance(instanceName, prefab)
  self.prefab = prefab

  self.path = self.tempPath

  if self.prefab ~= nil then
    self:showPrefabData(true)
  end

  self.selectedImage = nil
  self:hideImageGUI()

end

function PieceMode:showAvailableFolders()
  if self.folderLabel ~= nil then
    local folders = MOAIFileSystem.listDirectories(self.path)
    if folders ~= nil then
      local temp = ""
      for i = 1, #folders do
        temp = temp .. " " .. folders[i]
      end
      self.folderLabel:setText("Folders: " .. temp)
    elseif folders == nil then
      self.folderLabel:setText("Folders: ")
    end
  end
end

function PieceMode:createPipeLine()
  local PipeCreator = require "game.entities.pipecreator"
  local x,y = getCurState():getCamera():getLoc()
  local pipeCreator = PipeCreator.new(x,y, 8, 0.5)
  local line = pipeCreator:getPipeLine()
  pipeCreator:addPipe(0)
  pipeCreator:addPipe(-1)
  line:update()
  line:showInside()
  line:update()

end

return PieceMode
