<template>
  <div ref="container" class="three-background"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, inject } from 'vue'
import * as THREE from 'three'

const container = ref(null)
const isDarkTheme = inject('isDarkTheme', ref(true))

let scene, camera, renderer
let hearts = []
let clouds = []
let arrows = []
let cupid = null
let particles = []
let burstParticles = []
let mouseX = 0, mouseY = 0
let targetMouseX = 0, targetMouseY = 0
let animationId = null

// 颜色配置 - 粉红+玫红主题
const colors = {
  primary: 0xff69b4,      // 粉红
  secondary: 0xff1493,    // 玫红
  accent: 0xffb6c1,       // 浅粉
  light: 0xffc0cb,        // 淡粉
  gold: 0xffd700,         // 金色
  white: 0xffffff
}

// 初始化场景
const initScene = () => {
  if (!container.value) return

  // 创建场景
  scene = new THREE.Scene()
  
  // 创建相机
  const aspect = window.innerWidth / window.innerHeight
  camera = new THREE.PerspectiveCamera(60, aspect, 0.1, 1000)
  camera.position.z = 30

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ 
    antialias: true, 
    alpha: true 
  })
  renderer.setSize(window.innerWidth, window.innerHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(0x000000, 0)
  container.value.appendChild(renderer.domElement)

  // 添加灯光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(10, 10, 10)
  scene.add(directionalLight)

  const pointLight = new THREE.PointLight(colors.primary, 0.5, 50)
  pointLight.position.set(-10, 5, 10)
  scene.add(pointLight)

  // 创建场景元素
  createHearts()
  createClouds()
  createCupid()
  createArrows()
  createParticles()

  // 添加事件监听
  window.addEventListener('resize', onWindowResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('click', onMouseClick)

  // 开始动画循环
  animate()
}

// 创建爱心形状
const createHeartShape = () => {
  const shape = new THREE.Shape()
  const x = 0, y = 0
  
  shape.moveTo(x, y + 0.5)
  shape.bezierCurveTo(x, y + 0.5, x - 0.5, y, x - 0.5, y)
  shape.bezierCurveTo(x - 0.5, y - 0.5, x, y - 0.7, x, y - 1)
  shape.bezierCurveTo(x, y - 0.7, x + 0.5, y - 0.5, x + 0.5, y)
  shape.bezierCurveTo(x + 0.5, y, x, y + 0.5, x, y + 0.5)
  
  return shape
}

// 创建漂浮的爱心
const createHearts = () => {
  const heartShape = createHeartShape()
  const extrudeSettings = {
    depth: 0.3,
    bevelEnabled: true,
    bevelSegments: 3,
    bevelSize: 0.1,
    bevelThickness: 0.1
  }
  const geometry = new THREE.ExtrudeGeometry(heartShape, extrudeSettings)

  for (let i = 0; i < 20; i++) {
    const color = [colors.primary, colors.secondary, colors.accent, colors.light][Math.floor(Math.random() * 4)]
    const material = new THREE.MeshPhongMaterial({ 
      color: color,
      shininess: 100,
      transparent: true,
      opacity: 0.8
    })
    
    const heart = new THREE.Mesh(geometry, material)
    const scale = Math.random() * 0.8 + 0.3
    heart.scale.set(scale, scale, scale)
    heart.position.set(
      (Math.random() - 0.5) * 60,
      (Math.random() - 0.5) * 40,
      (Math.random() - 0.5) * 20 - 10
    )
    heart.rotation.set(
      Math.random() * Math.PI,
      Math.random() * Math.PI,
      Math.PI // 翻转爱心使其正向
    )
    
    // 存储动画参数
    heart.userData = {
      speed: Math.random() * 0.02 + 0.01,
      rotSpeed: Math.random() * 0.01 + 0.005,
      floatSpeed: Math.random() * 0.5 + 0.3,
      initialY: heart.position.y,
      phase: Math.random() * Math.PI * 2
    }
    
    hearts.push(heart)
    scene.add(heart)
  }
}

// 创建云朵
const createClouds = () => {
  const cloudGeometry = new THREE.SphereGeometry(1, 16, 16)
  
  for (let i = 0; i < 8; i++) {
    const cloudGroup = new THREE.Group()
    
    // 每个云由多个球体组成
    const numSpheres = Math.floor(Math.random() * 4) + 3
    for (let j = 0; j < numSpheres; j++) {
      const material = new THREE.MeshPhongMaterial({
        color: 0xffffff,
        transparent: true,
        opacity: 0.6
      })
      const sphere = new THREE.Mesh(cloudGeometry, material)
      const scale = Math.random() * 0.5 + 0.8
      sphere.scale.set(scale * 2, scale, scale)
      sphere.position.set(
        (Math.random() - 0.5) * 3,
        (Math.random() - 0.5) * 1,
        (Math.random() - 0.5) * 1
      )
      cloudGroup.add(sphere)
    }
    
    cloudGroup.position.set(
      (Math.random() - 0.5) * 80,
      (Math.random() - 0.5) * 30 + 10,
      -20 - Math.random() * 20
    )
    cloudGroup.userData = {
      speed: Math.random() * 0.01 + 0.005
    }
    
    clouds.push(cloudGroup)
    scene.add(cloudGroup)
  }
}

// 创建丘比特（简化版 - 用发光球体表示）
const createCupid = () => {
  const cupidGroup = new THREE.Group()
  
  // 丘比特身体 - 发光球体
  const bodyGeometry = new THREE.SphereGeometry(1, 32, 32)
  const bodyMaterial = new THREE.MeshPhongMaterial({
    color: colors.gold,
    emissive: colors.gold,
    emissiveIntensity: 0.3,
    shininess: 100
  })
  const body = new THREE.Mesh(bodyGeometry, bodyMaterial)
  cupidGroup.add(body)
  
  // 光环
  const haloGeometry = new THREE.TorusGeometry(1.5, 0.1, 16, 32)
  const haloMaterial = new THREE.MeshPhongMaterial({
    color: colors.gold,
    emissive: colors.gold,
    emissiveIntensity: 0.5
  })
  const halo = new THREE.Mesh(haloGeometry, haloMaterial)
  halo.position.y = 1.3
  halo.rotation.x = Math.PI / 2
  cupidGroup.add(halo)
  
  // 翅膀（用三角形表示）
  const wingShape = new THREE.Shape()
  wingShape.moveTo(0, 0)
  wingShape.lineTo(2, 1)
  wingShape.lineTo(2.5, 0)
  wingShape.lineTo(2, -0.5)
  wingShape.lineTo(0, 0)
  
  const wingGeometry = new THREE.ShapeGeometry(wingShape)
  const wingMaterial = new THREE.MeshPhongMaterial({
    color: 0xffffff,
    transparent: true,
    opacity: 0.8,
    side: THREE.DoubleSide
  })
  
  const leftWing = new THREE.Mesh(wingGeometry, wingMaterial)
  leftWing.position.set(-0.5, 0, 0)
  leftWing.rotation.y = Math.PI / 4
  cupidGroup.add(leftWing)
  
  const rightWing = new THREE.Mesh(wingGeometry, wingMaterial)
  rightWing.position.set(0.5, 0, 0)
  rightWing.rotation.y = -Math.PI / 4
  rightWing.scale.x = -1
  cupidGroup.add(rightWing)
  
  cupidGroup.position.set(15, 8, -5)
  cupidGroup.scale.set(0.8, 0.8, 0.8)
  cupidGroup.userData = {
    floatPhase: 0,
    wingPhase: 0
  }
  
  cupid = cupidGroup
  scene.add(cupidGroup)
}

// 创建飞舞的箭矢
const createArrows = () => {
  for (let i = 0; i < 5; i++) {
    const arrowGroup = new THREE.Group()
    
    // 箭身
    const shaftGeometry = new THREE.CylinderGeometry(0.05, 0.05, 3, 8)
    const shaftMaterial = new THREE.MeshPhongMaterial({ color: 0x8B4513 })
    const shaft = new THREE.Mesh(shaftGeometry, shaftMaterial)
    shaft.rotation.z = Math.PI / 2
    arrowGroup.add(shaft)
    
    // 箭头 - 爱心形状
    const heartShape = createHeartShape()
    const heartGeometry = new THREE.ShapeGeometry(heartShape)
    const heartMaterial = new THREE.MeshPhongMaterial({ 
      color: colors.secondary,
      side: THREE.DoubleSide
    })
    const arrowHead = new THREE.Mesh(heartGeometry, heartMaterial)
    arrowHead.position.x = 1.8
    arrowHead.scale.set(0.4, 0.4, 0.4)
    arrowHead.rotation.z = Math.PI
    arrowGroup.add(arrowHead)
    
    // 羽毛
    const featherGeometry = new THREE.ConeGeometry(0.2, 0.5, 4)
    const featherMaterial = new THREE.MeshPhongMaterial({ color: colors.accent })
    
    for (let j = 0; j < 3; j++) {
      const feather = new THREE.Mesh(featherGeometry, featherMaterial)
      feather.position.x = -1.5
      feather.position.y = (j - 1) * 0.15
      feather.rotation.z = Math.PI / 2 + (j - 1) * 0.2
      arrowGroup.add(feather)
    }
    
    arrowGroup.position.set(
      (Math.random() - 0.5) * 50,
      (Math.random() - 0.5) * 30,
      (Math.random() - 0.5) * 15 - 5
    )
    arrowGroup.rotation.z = Math.random() * Math.PI * 2
    
    arrowGroup.userData = {
      speed: Math.random() * 0.1 + 0.05,
      rotSpeed: Math.random() * 0.02,
      direction: new THREE.Vector3(
        Math.random() - 0.5,
        Math.random() - 0.5,
        0
      ).normalize()
    }
    
    arrows.push(arrowGroup)
    scene.add(arrowGroup)
  }
}

// 创建粒子系统
const createParticles = () => {
  const particleCount = 100
  const geometry = new THREE.BufferGeometry()
  const positions = new Float32Array(particleCount * 3)
  const colors_arr = new Float32Array(particleCount * 3)
  
  const color1 = new THREE.Color(colors.primary)
  const color2 = new THREE.Color(colors.secondary)
  
  for (let i = 0; i < particleCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 80
    positions[i * 3 + 1] = (Math.random() - 0.5) * 50
    positions[i * 3 + 2] = (Math.random() - 0.5) * 30
    
    const mixColor = Math.random() > 0.5 ? color1 : color2
    colors_arr[i * 3] = mixColor.r
    colors_arr[i * 3 + 1] = mixColor.g
    colors_arr[i * 3 + 2] = mixColor.b
  }
  
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('color', new THREE.BufferAttribute(colors_arr, 3))
  
  const material = new THREE.PointsMaterial({
    size: 0.3,
    vertexColors: true,
    transparent: true,
    opacity: 0.8,
    blending: THREE.AdditiveBlending
  })
  
  const particleSystem = new THREE.Points(geometry, material)
  particleSystem.userData = { phase: 0 }
  particles.push(particleSystem)
  scene.add(particleSystem)
}

// 创建爱心爆发效果
const createHeartBurst = (x, y) => {
  // 将屏幕坐标转换为 3D 坐标
  const vector = new THREE.Vector3(
    (x / window.innerWidth) * 2 - 1,
    -(y / window.innerHeight) * 2 + 1,
    0.5
  )
  vector.unproject(camera)
  const dir = vector.sub(camera.position).normalize()
  const distance = -camera.position.z / dir.z
  const pos = camera.position.clone().add(dir.multiplyScalar(distance))
  
  // 创建爆发的小爱心
  const heartShape = createHeartShape()
  const geometry = new THREE.ShapeGeometry(heartShape)
  
  for (let i = 0; i < 15; i++) {
    const color = [colors.primary, colors.secondary, colors.accent][Math.floor(Math.random() * 3)]
    const material = new THREE.MeshPhongMaterial({
      color: color,
      transparent: true,
      opacity: 1,
      side: THREE.DoubleSide
    })
    
    const heart = new THREE.Mesh(geometry, material)
    const scale = Math.random() * 0.3 + 0.1
    heart.scale.set(scale, scale, scale)
    heart.position.copy(pos)
    heart.rotation.z = Math.PI // 翻转爱心
    
    // 随机爆发方向
    const angle = (i / 15) * Math.PI * 2
    const speed = Math.random() * 0.3 + 0.2
    heart.userData = {
      velocity: new THREE.Vector3(
        Math.cos(angle) * speed,
        Math.sin(angle) * speed + 0.1,
        (Math.random() - 0.5) * 0.2
      ),
      life: 1.0,
      decay: Math.random() * 0.02 + 0.01,
      rotSpeed: (Math.random() - 0.5) * 0.2
    }
    
    burstParticles.push(heart)
    scene.add(heart)
  }
}

// 动画循环
const animate = () => {
  animationId = requestAnimationFrame(animate)
  
  const time = Date.now() * 0.001
  
  // 平滑鼠标跟随
  mouseX += (targetMouseX - mouseX) * 0.05
  mouseY += (targetMouseY - mouseY) * 0.05
  
  // 相机微微跟随鼠标
  camera.position.x = mouseX * 5
  camera.position.y = mouseY * 3
  camera.lookAt(0, 0, 0)
  
  // 动画爱心
  hearts.forEach((heart) => {
    const { speed, rotSpeed, floatSpeed, initialY, phase } = heart.userData
    heart.rotation.y += rotSpeed
    heart.rotation.x += rotSpeed * 0.5
    heart.position.y = initialY + Math.sin(time * floatSpeed + phase) * 2
    heart.position.x += speed
    
    // 循环位置
    if (heart.position.x > 35) {
      heart.position.x = -35
    }
  })
  
  // 动画云朵
  clouds.forEach((cloud) => {
    cloud.position.x += cloud.userData.speed
    if (cloud.position.x > 50) {
      cloud.position.x = -50
    }
  })
  
  // 动画丘比特
  if (cupid) {
    cupid.userData.floatPhase += 0.02
    cupid.userData.wingPhase += 0.1
    cupid.position.y = 8 + Math.sin(cupid.userData.floatPhase) * 1.5
    cupid.position.x = 15 + Math.cos(cupid.userData.floatPhase * 0.5) * 3
    
    // 翅膀拍动 - 简化版，只是上下移动
    if (cupid.children[2]) {
      cupid.children[2].rotation.y = Math.PI / 4 + Math.sin(cupid.userData.wingPhase) * 0.3
    }
    if (cupid.children[3]) {
      cupid.children[3].rotation.y = -Math.PI / 4 - Math.sin(cupid.userData.wingPhase) * 0.3
    }
  }
  
  // 动画箭矢
  arrows.forEach((arrow) => {
    const { speed, rotSpeed, direction } = arrow.userData
    arrow.position.add(direction.clone().multiplyScalar(speed))
    arrow.rotation.z += rotSpeed
    
    // 循环位置
    if (Math.abs(arrow.position.x) > 30 || Math.abs(arrow.position.y) > 20) {
      arrow.position.set(
        (Math.random() - 0.5) * 50,
        (Math.random() - 0.5) * 30,
        (Math.random() - 0.5) * 15 - 5
      )
    }
  })
  
  // 动画粒子
  particles.forEach((p) => {
    p.userData.phase += 0.01
    p.rotation.y += 0.001
    const positions = p.geometry.attributes.position.array
    for (let i = 0; i < positions.length; i += 3) {
      positions[i + 1] += Math.sin(p.userData.phase + i) * 0.01
    }
    p.geometry.attributes.position.needsUpdate = true
  })
  
  // 动画爆发粒子
  for (let i = burstParticles.length - 1; i >= 0; i--) {
    const heart = burstParticles[i]
    const { velocity, decay, rotSpeed } = heart.userData
    
    heart.position.add(velocity)
    velocity.y -= 0.01 // 重力
    heart.rotation.z += rotSpeed
    heart.userData.life -= decay
    heart.material.opacity = heart.userData.life
    
    if (heart.userData.life <= 0) {
      scene.remove(heart)
      heart.geometry.dispose()
      heart.material.dispose()
      burstParticles.splice(i, 1)
    }
  }
  
  renderer.render(scene, camera)
}

// 窗口调整
const onWindowResize = () => {
  if (!camera || !renderer) return
  camera.aspect = window.innerWidth / window.innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(window.innerWidth, window.innerHeight)
}

// 鼠标移动
const onMouseMove = (event) => {
  targetMouseX = (event.clientX / window.innerWidth) * 2 - 1
  targetMouseY = -(event.clientY / window.innerHeight) * 2 + 1
}

// 鼠标点击 - 爱心爆发
const onMouseClick = (event) => {
  createHeartBurst(event.clientX, event.clientY)
}

// 清理资源
const cleanup = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  
  window.removeEventListener('resize', onWindowResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('click', onMouseClick)
  
  // 清理所有对象
  hearts.forEach(h => {
    scene.remove(h)
    h.geometry.dispose()
    h.material.dispose()
  })
  
  clouds.forEach(c => {
    scene.remove(c)
    c.traverse(obj => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) obj.material.dispose()
    })
  })
  
  arrows.forEach(a => {
    scene.remove(a)
    a.traverse(obj => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) obj.material.dispose()
    })
  })
  
  if (cupid) {
    scene.remove(cupid)
    cupid.traverse(obj => {
      if (obj.geometry) obj.geometry.dispose()
      if (obj.material) obj.material.dispose()
    })
  }
  
  particles.forEach(p => {
    scene.remove(p)
    p.geometry.dispose()
    p.material.dispose()
  })
  
  burstParticles.forEach(b => {
    scene.remove(b)
    b.geometry.dispose()
    b.material.dispose()
  })
  
  if (renderer) {
    renderer.dispose()
    if (container.value && renderer.domElement) {
      container.value.removeChild(renderer.domElement)
    }
  }
  
  hearts = []
  clouds = []
  arrows = []
  cupid = null
  particles = []
  burstParticles = []
}

onMounted(() => {
  initScene()
})

onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
.three-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: auto;
}
</style>
