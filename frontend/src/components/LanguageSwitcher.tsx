import React from 'react'
import { useTranslation } from 'react-i18next'
import {
  Menu,
  MenuItem,
  IconButton,
  Typography,
  Box,
  ListItemIcon,
  ListItemText
} from '@mui/material'
import { Language as LanguageIcon } from '@mui/icons-material'

const languages = [
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'vi', name: 'Tiếng Việt', flag: '🇻🇳' },
  { code: 'zh', name: 'Chinese', flag: '🇨🇳' },
  { code: 'ja', name: 'Japanese', flag: '🇯🇵' },
  { code: 'ko', name: 'Korean', flag: '🇰🇷' }
]

const LanguageSwitcher: React.FC = () => {
  const { t, i18n } = useTranslation()
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const handleLanguageChange = (languageCode: string) => {
    i18n.changeLanguage(languageCode)
    handleClose()
  }

  const currentLanguage = languages.find(lang => lang.code === i18n.language) || languages[0]

  return (
    <Box>
      <IconButton
        onClick={handleClick}
        sx={{
          color: 'inherit',
          '&:hover': {
            backgroundColor: 'rgba(255, 255, 255, 0.1)'
          }
        }}
        title={t('language.changeLanguage')}
      >
        <LanguageIcon />
        <Typography variant="caption" sx={{ ml: 0.5, fontSize: '0.75rem' }}>
          {currentLanguage.flag}
        </Typography>
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{
          sx: {
            minWidth: 200,
            mt: 1
          }
        }}
      >
        {languages.map((language) => (
          <MenuItem
            key={language.code}
            onClick={() => handleLanguageChange(language.code)}
            selected={i18n.language === language.code}
            sx={{
              '&.Mui-selected': {
                backgroundColor: 'primary.main',
                color: 'primary.contrastText',
                '&:hover': {
                  backgroundColor: 'primary.dark'
                }
              }
            }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>
              <Typography variant="h6">{language.flag}</Typography>
            </ListItemIcon>
            <ListItemText
              primary={language.name}
              primaryTypographyProps={{
                fontSize: '0.9rem',
                fontWeight: i18n.language === language.code ? 'bold' : 'normal'
              }}
            />
          </MenuItem>
        ))}
      </Menu>
    </Box>
  )
}

export default LanguageSwitcher 